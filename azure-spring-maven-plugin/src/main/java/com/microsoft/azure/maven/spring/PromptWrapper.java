/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.microsoft.azure.maven.spring.exception.NoResourcesAvailableException;
import com.microsoft.azure.maven.spring.exception.SpringConfigurationException;
import com.microsoft.azure.maven.spring.prompt.DefaultPrompter;
import com.microsoft.azure.maven.spring.prompt.IPrompter;
import com.microsoft.azure.maven.spring.prompt.InputValidationResult;
import com.microsoft.azure.maven.spring.utils.SneakyThrowUtils;
import com.microsoft.azure.maven.spring.utils.TemplateUtils;
import com.microsoft.azure.maven.utils.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PromptWrapper {
    private ExpressionEvaluator expressionEvaluator;
    private ObjectMapper mapper;
    private IPrompter prompt;
    private Map<String, Map<String, Object>> templates;
    private Map<String, JsonNode> schemas;
    private Map<String, Object> commonVariables;
    private JsonValidator validator;
    private Log log;

    public PromptWrapper(ExpressionEvaluator expressionEvaluator, Log log) {
        this.expressionEvaluator = expressionEvaluator;

        this.mapper = new ObjectMapper();
        this.prompt = new DefaultPrompter();
        validator = JsonSchemaFactory.byDefault().getValidator();
        this.log = log;
    }

    public void initialize() throws IOException {
        templates = new HashMap<>();
        schemas = new HashMap<>();
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
                    /* if only one options is avaiable,
                       when it is required, select it by defaut*/
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

    public String handle(String templateId, boolean autoApplyDefault) throws IOException, ExpressionEvaluationException {
        return handle(templateId, autoApplyDefault, null);
    }

    public String handle(String templateId, boolean autoApplyDefault, Object defaultValueCli) throws IOException, ExpressionEvaluationException {
        final Map<String, Object> variables = createVariableTables(templateId);
        final String resourceName = (String) variables.get("resource");
        String type = (String) variables.get("type");
        Object defaultObj = variables.get("default");
        final String propertyName = (String) variables.get("property");
        if (StringUtils.isNotBlank(resourceName)) {
            final Map<String, Object> schema = getSchema(resourceName, propertyName);
            variables.put("schema", schema);
            if (defaultObj == null) {
                defaultObj = schema.get("default");
            }

            if (StringUtils.isBlank(type)) {
                type = (String) schema.get("type");
            }
            if (defaultValueCli != null) {
                // valid against the property from cli parameter, if it passes, then we skip the configuration
                final String errorMessage = validateSchema(resourceName, type, propertyName, defaultValueCli.toString());
                if (errorMessage == null) {
                    return defaultValueCli.toString();
                }
                System.out.println(TextUtils
                        .yellow(String.format("Input validation failure for %s[%s[: ", propertyName, defaultValueCli.toString(), errorMessage)));
            }
        }

        if (autoApplyDefault) {
            return Objects.toString(defaultObj, null);
        }
        if (defaultObj instanceof String && ((String) defaultObj).contains("${")) {
            variables.put("evaluatedDefault", expressionEvaluator.evaluate((String) defaultObj));
        }
        final String finalType = type;
        final String promoteMessage = TemplateUtils.evalText("promote", variables);
        final String inputAfterValidate = prompt.promoteString(promoteMessage, Objects.toString(defaultObj, null), input -> {
            if ("boolean".equals(finalType)) {
                if (input.equalsIgnoreCase("Y")) {
                    return InputValidationResult.wrap("true");
                }
                if (input.equalsIgnoreCase("N")) {
                    return InputValidationResult.wrap("false");
                }
            }
            final String value;
            try {
                value = evaluateMavenExpression(input);
            } catch (ExpressionEvaluationException e) {
                return SneakyThrowUtils.sneakyThrow(e);
            }

            if (StringUtils.isBlank(resourceName)) {
                if ("boolean".equals(finalType)) {
                    if (value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("FALSE")) {
                        return InputValidationResult.wrap(input);
                    }

                    return InputValidationResult
                            .error(String.format("'%s' cannot be converted to a boolean value.", input == value ? input : input + " -> " + value));
                }

                if ("integer".equals(finalType)) {
                    try {
                        Integer.parseInt(value);
                        return InputValidationResult.wrap(input);
                    } catch (NumberFormatException ex) {
                        return InputValidationResult.error(
                                String.format("'%s' cannot be converted to an integer value.", input == value ? input : input + " -> " + value));
                    }
                }

                return InputValidationResult.wrap(input);
            }
            final String errorMessage = validateSchema(resourceName, finalType, propertyName, value);
            return errorMessage == null ? InputValidationResult.wrap(input) : InputValidationResult.error(errorMessage);

        }, TemplateUtils.evalBoolean("required", variables));

        return inputAfterValidate;
    }

    public void confirmCommonHeader() {
        System.out.println(TemplateUtils.evalText("promote.header", createVariableTables("confirm")));
    }

    public boolean confirmCommonFooter(Log log) throws IOException {
        final Map<String, Object> variables = createVariableTables("confirm");
        final Boolean userConfirm = prompt.promoteYesNo(
                TemplateUtils.evalText("promote.footer", variables),
                TemplateUtils.evalBoolean("default", variables),
                TemplateUtils.evalBoolean("required", variables));
        if (userConfirm == null || !userConfirm.booleanValue()) {
            log.info(TemplateUtils.evalText("message.skip", variables));
            return false;
        }
        return true;
    }

    public void printConfirmResult(int size, Log log) {
        final Map<String, Object> variables = createVariableTables("confirm");
        if (size == 1) {
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

    private String validateSchema(String resourceName, String type, String name, String value) {
        try {
            final ProcessingReport reports = validator.validate(this.schemas.get(resourceName),
                    mapper.valueToTree(Collections.singletonMap(name, convertToType(type, value))));
            if (reports.isSuccess()) {
                return null;
            }

            final List<String> errors = new ArrayList<>();
            for (final ProcessingMessage pm : reports) {
                errors.add(pm.getMessage());
            }
            if (errors.size() == 1) {
                return errors.get(0);
            }
            return String.format("The input violates the validation rules:\n %s", errors.stream().collect(Collectors.joining("\n")));

        } catch (IllegalArgumentException | ProcessingException e) {
            return e.getMessage();
        }
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

    private Map<String, Object> getSchema(String resourceName, String property) throws IOException {
        final JsonNode schemaRoot = this.schemas.computeIfAbsent(resourceName,
            t -> {
                try {
                    return JsonLoader.fromResource("/schema/" + resourceName + ".json");
                } catch (IOException e) {
                    return SneakyThrowUtils.sneakyThrow(e);
                }
            });
        final JsonNode propertyJson = schemaRoot.get("properties").get(property);
        return mapper.treeToValue(propertyJson, Map.class);
    }

    private static Object convertToType(String type, String value) {
        if ("string".equals(type)) {
            return value;
        }
        if ("integer".equals(type)) {
            try {
                return Integer.parseInt(value);
            } catch (Exception ex) {
            }
        }
        if ("boolean".equals(type)) {
            try {
                return Boolean.parseBoolean(value);
            } catch (Exception ex) {

            }
        }
        return value;
    }
}
