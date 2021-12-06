/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.validator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_CREATORS;
import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_GETTERS;
import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_IS_GETTERS;

public class SchemaValidator {
    private static final Path SCHEMA_ROOT = Paths.get("schema");
    private static final String INVALID_PARAMETER_ERROR_MESSAGE = "Invalid parameters founded, please correct the value with messages below:";

    private final Map<String, JsonSchema> schemaMap = new HashMap<>();
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(AUTO_DETECT_CREATORS, AUTO_DETECT_GETTERS, AUTO_DETECT_IS_GETTERS);

    static {
        // disable invalid warning for schema key word `then`
        System.setProperty("org.slf4j.simpleLogger.log.com.networknt.schema.JsonMetaSchema", "off");
        // disable diagnostic info from Reflections
        System.setProperty("org.slf4j.simpleLogger.log.org.reflections.Reflections", "off");
    }

    private SchemaValidator() {
        Optional.of(new Reflections("schema", Scanners.Resources))
                .map(reflections -> {
                    try {
                        return reflections.getResources(".*\\.json");
                    } catch (Exception exception) {
                        return null;
                    }
                })
                .orElse(Collections.emptySet())
                .stream().map(resource -> Pair.of(resource, SchemaValidator.class.getResourceAsStream("/" + resource)))
                .filter(pair -> pair.getValue() != null)
                .forEach(pair -> registerSchema(getSchemaId(pair.getKey()), pair.getValue()));
    }

    public static SchemaValidator getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void registerSchema(@Nonnull final String schemaId, @Nonnull final JsonNode schema) {
        if (schemaMap.containsKey(schemaId)) {
            AzureMessager.getMessager().info(AzureString.format("Updating schema for %s", schemaId));
        }
        schemaMap.put(schemaId, factory.getSchema(schema));
    }

    public void registerSchema(@Nonnull final String schemaId, @Nonnull final InputStream schema) {
        try (final InputStream inputStream = schema) {
            final JsonNode schemaNode = this.objectMapper.readTree(inputStream);
            registerSchema(schemaId, schemaNode);
        } catch (IOException e) {
            AzureMessager.getMessager().warning(AzureString.format("Failed to load configuration schema %s", schemaId));
        }
    }

    public List<ValidationMessage> validate(@Nonnull final String schemaId, @Nonnull final Object value) {
        return validate(schemaId, value, "$");
    }

    public List<ValidationMessage> validate(@Nonnull final String schemaId, @Nonnull final Object value, @Nullable final String pathPrefix) {
        return validate(schemaId, objectMapper.convertValue(value, JsonNode.class), pathPrefix);
    }

    public List<ValidationMessage> validate(@Nonnull final String schemaId, @Nonnull final JsonNode value, @Nullable final String pathPrefix) {
        if (!schemaMap.containsKey(schemaId)) {
            AzureMessager.getMessager().warning(AzureString.format("Skip validation as schema %s was not registered", schemaId));
            return Collections.emptyList();
        }
        return schemaMap.get(schemaId).validate(value, value, pathPrefix).stream().map(ValidationMessage::fromRawMessage).collect(Collectors.toList());
    }

    public void validateAndThrow(@Nonnull final String schemaId, @Nonnull final Object value) {
        validateAndThrow(schemaId, value, "$");
    }

    public void validateAndThrow(@Nonnull final String schemaId, @Nonnull final Object value, @Nullable final String pathPrefix) {
        validateAndThrow(schemaId, objectMapper.convertValue(value, JsonNode.class), pathPrefix);
    }

    public void validateAndThrow(@Nonnull final String schemaId, @Nonnull final JsonNode value, @Nullable final String pathPrefix) {
        final List<ValidationMessage> result = validate(schemaId, value, pathPrefix);
        if (CollectionUtils.isNotEmpty(result)) {
            final String errorDetails = result.stream().map(message -> message.getMessage().toString()).collect(Collectors.joining(StringUtils.LF));
            throw new AzureToolkitRuntimeException(String.join(StringUtils.LF, INVALID_PARAMETER_ERROR_MESSAGE, errorDetails));
        }
    }

    private static String getSchemaId(final String path) {
        try {
            final Path schemaPath = Paths.get(FilenameUtils.removeExtension(path));
            final Path relativePath = SCHEMA_ROOT.relativize(schemaPath);
            return FilenameUtils.separatorsToUnix(relativePath.toString());
        } catch (IllegalArgumentException e) {
            // fallback to schema path for path parse issue
            return path;
        }
    }

    private static class LazyHolder {
        static final SchemaValidator INSTANCE = new SchemaValidator();
    }
}
