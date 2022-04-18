/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AzureTelemetry {
    public static final String OP_CREATE_AT = "op_create_at";
    public static final String OP_ENTER_AT = "op_enter_at";
    public static final String OP_EXIT_AT = "op_exit_at";

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
}
