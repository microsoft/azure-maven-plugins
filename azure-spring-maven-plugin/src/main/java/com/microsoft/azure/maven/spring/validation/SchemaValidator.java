/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.google.common.base.Preconditions;
import com.microsoft.azure.maven.spring.utils.SneakyThrowUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SchemaValidator {
    private ObjectMapper mapper;
    private Map<String, JsonNode> schemas = new HashMap<>();
    private JsonValidator validator;

    public SchemaValidator() {
        this.mapper = new ObjectMapper();
        validator = JsonSchemaFactory.byDefault().getValidator();
    }

    public String validateSchema(String resourceName, String name, String value) {
        Preconditions.checkArgument(StringUtils.isNoneBlank(resourceName), "Parameter 'resource' should not be null or empty.");
        Preconditions.checkArgument(StringUtils.isNoneBlank(name), "Parameter 'name' should not be null or empty.");
        try {
            final JsonNode schema = this.schemas.get(resourceName);
            final String type = schema.get("properties").get(name).get("type").asText();
            if (StringUtils.isBlank(type)) {
                return "Invalid schema configuration for property " + name;
            }
            final ProcessingReport reports = validator.validate(schema,
                    mapper.valueToTree(Collections.singletonMap(name, stringToObject(type, value))));
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

    public Map<String, Object> getSchemaMap(String resourceName, String property) throws IOException {
        final JsonNode schemaRoot = this.schemas.computeIfAbsent(resourceName, t -> {
            try {
                return JsonLoader.fromResource("/schema/" + resourceName + ".json");
            } catch (IOException e) {
                return SneakyThrowUtils.sneakyThrow(e);
            }
        });
        final JsonNode propertyJson = schemaRoot.get("properties").get(property);
        return mapper.treeToValue(propertyJson, Map.class);
    }

    private static Object stringToObject(String type, String value) {
        if ("string".equals(type)) {
            return value;
        }
        if ("integer".equals(type)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(String.format("%s cannot be converted to an integer", value));
            }
        }
        if ("boolean".equals(type)) {
            if ("true".equalsIgnoreCase(value)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(value)) {
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException(String.format("%s cannot be converted to a boolean value.", value));
        }
        throw new IllegalArgumentException(String.format("Type '%s' is not supported in schema validation.", type));
    }
}
