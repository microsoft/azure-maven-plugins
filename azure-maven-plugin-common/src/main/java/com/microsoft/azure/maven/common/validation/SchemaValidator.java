/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.common.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SchemaValidator {

    private Map<String, Map<String, Object>> schemaMap = new HashMap<>();

    private Map<String, JsonNode> schemas = new HashMap<>();

    private final JsonValidator validator;

    private final ObjectMapper mapper;

    public SchemaValidator() {
        mapper = new ObjectMapper();
        validator = JsonSchemaFactory.byDefault().getValidator();
    }

    public void collectSchemaForObject(String resource, JsonNode schema) throws JsonProcessingException {
        Preconditions.checkArgument(StringUtils.isNotBlank(resource), "Parameter 'resource' should not be null or empty.");
        Preconditions.checkArgument(!schemaMap.containsKey(resource),
                String.format("Resource '%s' already exists.", resource));
        Preconditions.checkNotNull(schema, "Parameter 'resource' should not be null.");

        schemas.put(resource, schema);
        schemaMap.put(resource, mapper.treeToValue(schema, Map.class));

    }

    public void collectSingleProperty(String resource, String property, JsonNode schema) throws JsonProcessingException {
        Preconditions.checkArgument(StringUtils.isNotBlank(resource), "Parameter 'resource' should not be null or empty.");
        Preconditions.checkArgument(StringUtils.isNotBlank(property), "Parameter 'property' should not be null or empty.");
        Preconditions.checkArgument(!schemaMap.containsKey(combineToKey(resource, property)),
                String.format("Duplicate property '%s'.", combineToKey(resource, property)));

        schemas.put(combineToKey(resource, property), schema);
        schemaMap.put(combineToKey(resource, property), mapper.treeToValue(schema, Map.class));
    }

    public Map<String, Object> getSchemaMapForObject(String resource) {
        Preconditions.checkArgument(StringUtils.isNotBlank(resource), "Parameter 'resource' should not be null or empty.");
        Preconditions.checkArgument(schemaMap.containsKey(resource),
                String.format("Schema for '%s' cannot be found.", resource));
        return schemaMap.get(resource);
    }

    public Map<String, Object> getSchemaMap(String resource, String property) {
        Preconditions.checkArgument(StringUtils.isNotBlank(resource), "Parameter 'resource' should not be null or empty.");
        Preconditions.checkArgument(StringUtils.isNotBlank(property), "Parameter 'property' should not be null or empty.");
        Preconditions.checkArgument(schemaMap.containsKey(combineToKey(resource, property)),
                String.format("Property '%s' cannot be found.", combineToKey(resource, property)));

        return schemaMap.get(combineToKey(resource, property));
    }

    public String validateObject(String resource, JsonNode value) {
        Preconditions.checkArgument(StringUtils.isNotBlank(resource), "Parameter 'resource' should not be null or empty.");
        Preconditions.checkArgument(schemaMap.containsKey(resource),
                String.format("Schema for '%s' cannot be found.", resource));

        final JsonNode schema = this.schemas.get(resource);
        try {
            final ProcessingReport reports = validator.validate(schema, value);
            return formatValidationResults(reports);
        } catch (IllegalArgumentException | ProcessingException e) {
            return e.getMessage();
        }

    }

    public String validateSingleProperty(String resource, String property, String value) {
        Preconditions.checkArgument(StringUtils.isNotBlank(resource), "Parameter 'resource' should not be null or empty.");
        Preconditions.checkArgument(StringUtils.isNotBlank(property), "Parameter 'property' should not be null or empty.");
        Preconditions.checkArgument(schemas.containsKey(combineToKey(resource, property)),
                String.format("Property '%s' cannot be found.", combineToKey(resource, property)));
        final JsonNode schema = this.schemas.get(combineToKey(resource, property));
        final String type = (String) schemaMap.get(combineToKey(resource, property)).get("type");
        try {
            final ProcessingReport reports = validator.validate(schema, stringToJsonObject(type, value));
            return formatValidationResults(reports);
        } catch (IllegalArgumentException | ProcessingException e) {
            return e.getMessage();
        }
    }

    private String combineToKey(String resource, String property) {
        return resource + "::" + property;
    }

    private static JsonNode stringToJsonObject(String type, String value) {
        if ("string".equals(type)) {
            return TextNode.valueOf(value);
        }
        if ("integer".equals(type)) {
            try {
                return IntNode.valueOf(Integer.parseInt(value));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(String.format("%s cannot be converted to an integer", value));
            }
        }
        if ("boolean".equals(type)) {
            if ("true".equalsIgnoreCase(value)) {
                return BooleanNode.TRUE;
            }
            if ("false".equalsIgnoreCase(value)) {
                return BooleanNode.FALSE;
            }
            throw new IllegalArgumentException(String.format("%s cannot be converted to a boolean value.", value));
        }
        throw new IllegalArgumentException(String.format("Type '%s' is not supported in schema validation.", type));
    }

    private String formatValidationResults(final ProcessingReport reports) {
        if (reports.isSuccess()) {
            return null;
        }
        final List<String> errors = new ArrayList<>();
        for (final ProcessingMessage pm : reports) {
            if (pm.asJson().has("keyword")) {
                errors.add(String.format("Keyword: %s, Detail: %s", pm.asJson().get("keyword"), pm.getMessage()));
            } else {
                errors.add(pm.getMessage());
            }

        }
        if (errors.size() == 1) {
            return errors.get(0);
        }
        return String.format("The input violates the validation rules:\n %s", errors.stream().collect(Collectors.joining("\n")));
    }

}
