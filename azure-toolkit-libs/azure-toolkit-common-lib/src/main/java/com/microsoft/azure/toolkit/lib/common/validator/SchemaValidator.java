/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.validator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_CREATORS;
import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_GETTERS;
import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_IS_GETTERS;

public class SchemaValidator {

    private final Map<String, JsonSchema> schemaMap = new HashMap<>();
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(AUTO_DETECT_CREATORS, AUTO_DETECT_GETTERS, AUTO_DETECT_IS_GETTERS);

    static {
        // disable invalid warning for schema key word `then`
        System.setProperty("org.slf4j.simpleLogger.log.com.networknt.schema.JsonMetaSchema", "off");
    }

    private SchemaValidator() {
        final Set<String> resources = new Reflections("schema", new ResourcesScanner()).getResources(Pattern.compile(".*\\.json"));
        resources.stream().map(resource -> Pair.of(resource, SchemaValidator.class.getResourceAsStream("/" + resource)))
                .filter(pair -> pair.getValue() != null)
                .forEach(pair -> registerSchema(FilenameUtils.getBaseName(pair.getKey()), pair.getValue()));
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

    public List<ValidationMessage> validate(@Nonnull final String schemaId, @Nonnull final JsonNode value) {
        return validate(schemaId, value, "$");
    }

    public List<ValidationMessage> validate(@Nonnull final String schemaId, @Nonnull final JsonNode value, @Nullable final String pathPrefix) {
        if (!schemaMap.containsKey(schemaId)) {
            AzureMessager.getMessager().warning(AzureString.format("Skip validation as schema %s was not registered", schemaId));
            return Collections.emptyList();
        }
        return validate(schemaMap.get(schemaId), value, pathPrefix);
    }

    private List<ValidationMessage> validate(@Nonnull final JsonSchema schema, @Nonnull final JsonNode value, @Nullable final String pathPrefix) {
        return schema.validate(value, value, pathPrefix).stream().map(ValidationMessage::fromRawMessage).collect(Collectors.toList());
    }

    private static class LazyHolder {
        static final SchemaValidator INSTANCE = new SchemaValidator();
    }
}
