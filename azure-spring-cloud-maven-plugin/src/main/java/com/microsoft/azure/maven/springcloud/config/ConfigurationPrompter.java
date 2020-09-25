/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.microsoft.azure.common.prompt.DefaultPrompter;
import com.microsoft.azure.common.prompt.IPrompter;
import com.microsoft.azure.common.prompt.InputValidateResult;
import com.microsoft.azure.common.utils.SneakyThrowUtils;
import com.microsoft.azure.common.utils.TextUtils;
import com.microsoft.azure.common.validation.SchemaValidator;
import com.microsoft.azure.maven.utils.TemplateUtils;

import com.microsoft.azure.tools.exception.InvalidConfigurationException;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfigurationPrompter {
    private ExpressionEvaluator expressionEvaluator;
    private IPrompter prompt;
    private Map<String, Map<String, Object>> templates;
    private Map<String, Object> commonVariables;
    private SchemaValidator validator;
    private Log log;

    public ConfigurationPrompter(ExpressionEvaluator expressionEvaluator, Log log) {
        this.expressionEvaluator = expressionEvaluator;
        this.log = log;
    }

    public void initialize() throws IOException, InvalidConfigurationException {
        prompt = new DefaultPrompter();
        validator = new SchemaValidator();
        templates = new HashMap<>();
        commonVariables = new HashMap<>();
        final Yaml yaml = new Yaml();
        final Set<String> resourceNames = new HashSet<>();
        try (final InputStream inputStream = this.getClass().getResourceAsStream("/MessageTemplates.yaml")) {
            final Iterable<Object> rules = yaml.loadAll(inputStream);

            for (final Object rule : rules) {
                final Map<String, Object> map = (Map<String, Object>) rule;
                templates.put((String) map.get("id"), map);
                if (map.containsKey("resource")) {
                    resourceNames.add((String) map.get("resource"));
                }
            }
        }
        for (final String resourceName : resourceNames) {
            final ObjectNode resourceSchema = (ObjectNode) JsonLoader.fromResource("/schema/" + resourceName + ".json");
            if (!resourceSchema.has("properties")) {
                throw new InvalidConfigurationException(String.format("Bad schema for %s: missing properties field.", resourceName));
            }
            final ObjectNode propertiesNode = (ObjectNode) resourceSchema.get("properties");
            IteratorUtils.forEach(propertiesNode.fields(), prop -> {
                try {
                    this.validator.collectSingleProperty(resourceName, prop.getKey(), prop.getValue());
                } catch (JsonProcessingException e) {
                    SneakyThrowUtils.sneakyThrow(e);
                }
            });
        }

    }

    public void putCommonVariable(String key, Object obj) {
        this.commonVariables.put(key, obj);
    }

    public <T> T handleSelectOne(String templateId, List<T> options, T defaultEntity, Function<T, String> getNameFunc)
            throws IOException, InvalidConfigurationException {
        final Map<String, Object> variables = createVariableTables(templateId);
        final boolean isRequired = TemplateUtils.evalBoolean("required", variables);
        if (options.size() == 0) {
            if (isRequired) {
                throw new InvalidConfigurationException(TemplateUtils.evalText("message.empty_options", variables));
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
                    /*
                     * if only one options is available, when it is required, select it by default
                     */
                    isRequired, isRequired)) {
                if (isRequired) {
                    throw new InvalidConfigurationException(TemplateUtils.evalText("message.select_none", variables));
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
        throws IOException, InvalidConfigurationException {
        final Map<String, Object> variables = createVariableTables(templateId);
        final boolean allowEmpty = TemplateUtils.evalBoolean("allow_empty", variables);
        if (options.size() == 0) {
            if (!allowEmpty) {
                throw new InvalidConfigurationException(TemplateUtils.evalText("message.empty_options", variables));
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
                TemplateUtils.evalText("promote.many", variables), TemplateUtils.evalText("promote.header", variables), options, getNameFunc,
                allowEmpty, defaultSelected ? "to select ALL" : "to select NONE", defaultSelected ? options : Collections.emptyList());
        if (selectedEntities.isEmpty()) {
            final String warningMessage = TemplateUtils.evalText("message.select_none", variables);
            if (StringUtils.isNotBlank(warningMessage)) {
                log.warn(warningMessage);
            }
        }
        return selectedEntities;
    }

    public String handle(String templateId, boolean autoApplyDefault)
            throws InvalidConfigurationException, IOException {
        return handle(templateId, autoApplyDefault, null);
    }

    public String handle(String templateId, boolean autoApplyDefault, Object cliParameter)
            throws InvalidConfigurationException, IOException {
        final Map<String, Object> variables = createVariableTables(templateId);
        final String resourceName = (String) variables.get("resource");

        final String propertyName = (String) variables.get("property");
        if (StringUtils.isBlank(propertyName)) {
            throw new InvalidConfigurationException("Cannot find property in template: " + templateId);
        }
        if (StringUtils.isBlank(resourceName)) {
            throw new InvalidConfigurationException("Cannot find resource in template: " + templateId);
        }
        final Map<String, Object> schema = validator.getSchemaMap(resourceName, propertyName);
        variables.put("schema", schema);
        Object defaultObj = variables.get("default");
        if (defaultObj instanceof String) {
            defaultObj = TemplateUtils.evalPlainText("default", variables);
        } else {
            if (defaultObj == null) {
                defaultObj = schema.get("default");
            }
        }
        final String defaultObjectStr = Objects.toString(defaultObj, null);
        final String type = (String) schema.get("type");

        if (cliParameter != null) {
            // valid against the property from cli parameter, if it passes, then we skip the configuration
            final String errorMessage = validator.validateSingleProperty(resourceName, propertyName, cliParameter.toString());
            if (errorMessage == null) {
                return cliParameter.toString();
            }
            System.out.println(
                    TextUtils.yellow(String.format("Validation failure for %s[%s]: %s", propertyName, cliParameter.toString(), errorMessage)));
        }

        if (autoApplyDefault) {
            if (defaultObj != null) {
                // we need to check default value
                final String errorMessage = validator.validateSingleProperty(resourceName, propertyName, defaultObjectStr);
                if (errorMessage == null) {
                    return defaultObjectStr;
                }
                throw new InvalidConfigurationException(
                        String.format("Default value '%s' cannot be applied to %s due to error: %s", defaultObjectStr, propertyName, errorMessage));
            }

            return null;
        }

        final String promoteMessage = TemplateUtils.evalText("promote", variables);
        return prompt.promoteString(promoteMessage, Objects.toString(defaultObj, null), input -> {
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
                    return InputValidateResult.error(String.format("Cannot evaluate maven expression: %s", input));
                }
            } catch (ExpressionEvaluationException e) {
                return InputValidateResult.error(e.getMessage());
            }

            final String errorMessage = validator.validateSingleProperty(resourceName, propertyName, value);
            return errorMessage == null ? InputValidateResult.wrap(input) : InputValidateResult.error(errorMessage);

        }, TemplateUtils.evalBoolean("required", variables));
    }

    public void confirmChanges(Map<String, String> changesToConfirm, Supplier<Integer> confirmedAction) throws IOException {
        final Map<String, Object> variables = createVariableTables("confirm");
        System.out.println(TemplateUtils.evalText("promote.header", variables));
        for (final Map.Entry<String, String> entry : changesToConfirm.entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue())) {
                printConfirmation(entry.getKey(), entry.getValue());
            }
        }

        final Boolean userConfirm = prompt.promoteYesNo(TemplateUtils.evalText("promote.footer", variables),
                TemplateUtils.evalBoolean("default", variables), TemplateUtils.evalBoolean("required", variables));
        if (userConfirm == null || !userConfirm) {
            log.info(TemplateUtils.evalText("message.skip", variables));
            return;
        }
        final Integer appliedCount = confirmedAction.get();
        if (appliedCount == null || appliedCount == 0) {
            log.info(TemplateUtils.evalText("message.none", variables));
        } else if (appliedCount == 1) {
            log.info(TemplateUtils.evalText("message.one", variables));
        } else {
            log.info(TemplateUtils.evalText("message.many", variables));
        }
    }

    public void close() throws IOException {
        this.prompt.close();
    }

    private Map<String, Object> createVariableTables(String templateId) {
        final Map<String, Object> variables = templates.get(templateId);
        if (variables == null) {
            throw new IllegalArgumentException("Cannot find template: " + templateId);
        }
        variables.putAll(this.commonVariables);
        return variables;
    }

    private String evaluateMavenExpression(String input) throws ExpressionEvaluationException {
        if (input != null && input.contains("${")) {
            return (String) expressionEvaluator.evaluate(input);
        }
        return input;
    }

    private static void printConfirmation(String key, Object value) {
        System.out.printf("%-17s : %s%n", key, TextUtils.green(Objects.toString(value)));
    }
}
