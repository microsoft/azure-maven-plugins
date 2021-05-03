/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AzureTelemetry {
    static final String OP_CREATE_AT = "op_create_at";
    static final String OP_ENTER_AT = "op_enter_at";
    static final String OP_EXIT_AT = "op_exit_at";

    public enum Type {
        OP_START,
        OP_END,
        STEP,
        INFO,
        WARNING,
        ERROR
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Property {

        String PARAM_NAME = "<param_name>";

        /**
         * alias of {@code name}
         */
        String value() default PARAM_NAME;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Properties {

        /**
         * alias of {@code converter}
         */
        Class<? extends Converter> value() default DefaultConverter.class;

        interface Converter<T> {
            @Nonnull
            Map<String, String> convert(@Nullable T obj);
        }

        class DefaultConverter implements Converter<Object> {
            private static final ObjectMapper objectMapper = new ObjectMapper();
            private static final TypeReference<Map<String, String>> type = new TypeReference<Map<String, String>>() {
            };

            @Override
            @Nonnull
            public Map<String, String> convert(@Nullable Object obj) {
                return Optional.ofNullable(obj).map(o -> objectMapper.convertValue(o, type)).orElse(new HashMap<>());
            }
        }
    }

    @Nonnull
    public static Context getContext() {
        return getContext(IAzureOperation.current());
    }

    @Nonnull
    public static AzureTelemetry.Context getContext(@Nullable IAzureOperation operation) {
        return Optional.ofNullable(operation)
                .map(o -> o.get(AzureTelemetry.Context.class, new AzureTelemetry.Context(operation)))
                .orElse(new AzureTelemetry.Context(operation));
    }

    @Getter
    @RequiredArgsConstructor
    public static class Context implements IAzureOperation.IContext {
        private final Map<String, String> properties = new ConcurrentHashMap<>();
        @Nullable
        private final IAzureOperation operation;

        public void setCreateAt(Instant createAt) {
            this.properties.put(OP_CREATE_AT, createAt.toString());
        }

        public void setEnterAt(Instant enterAt) {
            this.properties.put(OP_ENTER_AT, enterAt.toString());
        }

        public void setExitAt(Instant exitAt) {
            this.properties.put(OP_EXIT_AT, exitAt.toString());
        }

        public void setProperty(String key, String val) {
            this.properties.put(key, val);
        }

        public void setProperties(Map<String, String> properties) {
            this.properties.putAll(properties);
        }

        public String getProperty(String key) {
            return this.properties.get(key);
        }

        @Nonnull
        public Map<String, String> getActionProperties() {
            return Optional.ofNullable(this.operation)
                    .map(IAzureOperation::getActionParent)
                    .map(o -> o.get(AzureTelemetry.Context.class, new AzureTelemetry.Context(o)))
                    .map(Context::getProperties)
                    .orElse(new HashMap<>());
        }
    }
}
