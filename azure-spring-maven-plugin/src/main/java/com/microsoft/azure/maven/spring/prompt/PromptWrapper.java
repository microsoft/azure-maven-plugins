/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.prompt;

import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.maven.spring.exception.NoResourcesAvailableException;
import com.microsoft.azure.maven.spring.exception.SpringConfigurationException;
import com.microsoft.azure.maven.spring.utils.TemplateUtils;
import com.microsoft.azure.maven.spring.validation.SchemaValidator;
import com.microsoft.azure.maven.utils.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class PromptWrapper {
    private ExpressionEvaluator expressionEvaluator;
    private IPrompter prompt;
    private Map<String, Map<String, Object>> templates;
    private Map<String, Object> commonVariables;
    private SchemaValidator validator;
    private Log log;

    public PromptWrapper(ExpressionEvaluator expressionEvaluator, Log log) {
        this.expressionEvaluator = expressionEvaluator;
        this.log = log;
    }

    public void initialize() throws IOException {
        prompt = new DefaultPrompter();
        validator = new SchemaValidator();
        templates = new HashMap<>();
        commonVariables = new HashMap<>();
        final Yaml yaml = new Yaml();
        try (final InputStream inputStream = this.getClass().getResourceAsStream("/MessageTemplates.yaml")) {
            final Iterable<Object> rules = yaml.loadAll(inputStream);

            for (final Object rule : rules) {
                final Map<String, Object> map = (Map<String, Object>) rule;
                templates.put((String) map.get("id"), map);
            }
        }
    }

    public void putCommonVariable(String key, Object obj) {
        this.commonVariables.put(key, obj);
    }

    public <T> T handleSelectOne(String templateId, List<T> options, T defaultEntity, Function<T, String> getNameFunc)
            throws IOException, SpringConfigurationException, NoResourcesAvailableException {
        final Map<String, Object> variables = createVariableTables(templateId);
        final boolean isRequired = TemplateUtils.evalBoolean("required", variables);
        if (options.size() == 0) {
            if (isRequired) {
                throw new NoResourcesAvailableException(TemplateUtils.evalText("message.empty_options", variables));
            }
            final String warningMessage = TemplateUtils.evalText("message.empty_options", variables);
            if (StringUtils.isNotBlank(warningMessage)) {
                log.warn(warningMessage);
            }
            return null;
        }
        final boolean autoSelect = TemplateUtils.evalBoolean("auto_select", variables);
        if (options.size() == 1) {
            if (autoSelect) {
                log.info(TemplateUtils.evalText("message.auto_select", variables));
                return options.get(0);
            }
            if (!this.prompt.promoteYesNo(TemplateUtils.evalText("promote.one", variables),
                    /* if only one options is available,
                       when it is required, select it by default*/
                    isRequired ,
                    isRequired)) {
                if (isRequired) {
                    throw new SpringConfigurationException(TemplateUtils.evalText("message.select_none", variables));
                }
                return null;
            }
            return options.get(0);
        }
        if (defaultEntity == null && variables.containsKey("default_index")) {
            defaultEntity = options.get((Integer) variables.get("default_index"));
        }
        return prompt.promoteSingleEntity(TemplateUtils.evalText("promote.header", variables), TemplateUtils.evalText("promote.many", variables),
                options, defaultEntity, getNameFunc, isRequired);
    }

    public <T> List<T> handleMultipleCase(String templateId, List<T> options, Function<T, String> getNameFunc)
            throws IOException, NoResourcesAvailableException {
        final Map<String, Object> variables = createVariableTables(templateId);
        final boolean allowEmpty = TemplateUtils.evalBoolean("allow_empty", variables);
        if (options.size() == 0) {
            if (!allowEmpty) {
                throw new NoResourcesAvailableException(TemplateUtils.evalText("message.empty_options", variables));
            } else {
                final String warningMessage = TemplateUtils.evalText("message.empty_options", variables);
                if (StringUtils.isNotBlank(warningMessage)) {
                    log.warn(warningMessage);
                }
            }
            return options;
        }
        final boolean autoSelect = TemplateUtils.evalBoolean("auto_select", variables);
        final boolean defaultSelected = TemplateUtils.evalBoolean("default_selected", variables);
        if (options.size() == 1) {
            if (autoSelect) {
                log.info(TemplateUtils.evalText("message.auto_select", variables));
                return options;
            } else {
                if (!this.prompt.promoteYesNo(TemplateUtils.evalText("promote.one", variables), defaultSelected, false)) {
                    // user cancels
                    final String warningMessage = TemplateUtils.evalText("message.select_none", variables);
                    if (StringUtils.isNotBlank(warningMessage)) {
                        log.warn(warningMessage);
                    }
                    return Collections.emptyList();
                }
                return options;
            }
        }
        final List<T> selectedEntities = prompt.promoteMultipleEntities(TemplateUtils.evalText("promote.header", variables),
                TemplateUtils.evalText("promote.many", variables),
                TemplateUtils.evalText("promote.header", variables), options, getNameFunc, allowEmpty,
                defaultSelected ? "to select ALL" : "to select NONE", defaultSelected ? options : Collections.emptyList());
        if (selectedEntities.isEmpty()) {
            final String warningMessage = TemplateUtils.evalText("message.select_none", variables);
            if (StringUtils.isNotBlank(warningMessage)) {
                log.warn(warningMessage);
            }
        }
        return selectedEntities;
    }

    public String handle(String templateId, boolean autoApplyDefault) throws InvalidConfigurationException, IOException, ExpressionEvaluationException {
        return handle(templateId, autoApplyDefault, null);
    }

    public String handle(String templateId, boolean autoApplyDefault, Object defaultValueCli)
            throws InvalidConfigurationException, IOException, ExpressionEvaluationException {
        final Map<String, Object> variables = createVariableTables(templateId);
        final String resourceName = (String) variables.get("resource");

        final String propertyName = (String) variables.get("property");
        if (StringUtils.isBlank(propertyName)) {
            throw new InvalidConfigurationException("Cannot find propertyName in template: " + templateId);
        }
        if (StringUtils.isBlank(resourceName)) {
            throw new InvalidConfigurationException("Cannot find schema for property " + propertyName);
        }
        final Map<String, Object> schema = validator.getSchemaMap(resourceName, propertyName);
        variables.put("schema", schema);
        Object defaultObj = variables.get("default");
        if (defaultObj == null) {
            defaultObj = schema.get("default");
        }

        final String type = (String) schema.get("type");

        if (defaultValueCli != null) {
            // valid against the property from cli parameter, if it passes, then we skip the configuration
            final String errorMessage = validator.validateSchema(resourceName, propertyName, defaultValueCli.toString());
            if (errorMessage == null) {
                return defaultValueCli.toString();
            }
            System.out.println(TextUtils
                    .yellow(String.format("Input validation failure for %s[%s[: ", propertyName, defaultValueCli.toString(), errorMessage)));
        }

        if (autoApplyDefault) {
            return Objects.toString(defaultObj, null);
        }
        if (defaultObj instanceof String && ((String) defaultObj).contains("${")) {
            variables.put("evaluatedDefault", expressionEvaluator.evaluate((String) defaultObj));
        }
        final String promoteMessage = TemplateUtils.evalText("promote", variables);
        final String inputAfterValidate = prompt.promoteString(promoteMessage, Objects.toString(defaultObj, null), input -> {
            if ("boolean".equals(type)) {
                // convert user input from y to true and N to false
                if (input.equalsIgnoreCase("Y")) {
                    input = "true";
                }
                if (input.equalsIgnoreCase("N")) {
                    input = "false";
                }
            }
            final String value;
            try {
                value = evaluateMavenExpression(input);
                if (value == null) {
                    return InputValidationResult.error(String.format("Cannot evaluate maven expression: %s", input));
                }
            } catch (ExpressionEvaluationException e) {
                return InputValidationResult.error(e.getMessage());
            }

            final String errorMessage = validator.validateSchema(resourceName, propertyName, value);
            return errorMessage == null ? InputValidationResult.wrap(input) : InputValidationResult.error(errorMessage);

        }, TemplateUtils.evalBoolean("required", variables));

        return inputAfterValidate;
    }

    public void confirmChanges(Map<String, String> changesToConfirm, Supplier<Integer> confirmedAction) throws IOException {
        final Map<String, Object> variables = createVariableTables("confirm");
        System.out.println(TemplateUtils.evalText("promote.header", variables));
        for (final Map.Entry<String, String> entry : changesToConfirm.entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue())) {
                printConfirmation(entry.getKey(), entry.getValue());
            }
        }

        final Boolean userConfirm = prompt.promoteYesNo(
                TemplateUtils.evalText("promote.footer", variables),
                TemplateUtils.evalBoolean("default", variables),
                TemplateUtils.evalBoolean("required", variables));
        if (userConfirm == null || !userConfirm.booleanValue()) {
            log.info(TemplateUtils.evalText("message.skip", variables));
            return;
        }
        final Integer appliedCount = confirmedAction.get();
        if (appliedCount == null || appliedCount.intValue() == 0) {
            log.info(TemplateUtils.evalText("message.none", variables));
        } else if (appliedCount.intValue() == 1) {
            log.info(TemplateUtils.evalText("message.one", variables));
        } else {
            log.info(TemplateUtils.evalText("message.many", variables));
        }
    }

    public void close() throws IOException {
        this.prompt.close();
    }

    private Map<String, Object> createVariableTables(String templateId) {
        final Map<String, Object> templateById = templates.get(templateId);
        if (templateById == null) {
            throw new IllegalArgumentException("Cannot find template: " + templateId);
        }

        return this.mergeCommonProperties(templateById);
    }

    private String evaluateMavenExpression(String input) throws ExpressionEvaluationException {
        if (input != null && input.contains("${")) {
            return (String) expressionEvaluator.evaluate(input);
        }
        return input;
    }

    private Map<String, Object> mergeCommonProperties(Map<String, Object> map) {
        for (final Map.Entry<String, Object> entity : commonVariables.entrySet()) {
            map.put(entity.getKey(), entity.getValue());
        }
        return map;
    }

    private static void printConfirmation(String key, Object value) {
        System.out.printf("%-17s : %s%n", key, TextUtils.green(Objects.toString(value)));
    }
}
