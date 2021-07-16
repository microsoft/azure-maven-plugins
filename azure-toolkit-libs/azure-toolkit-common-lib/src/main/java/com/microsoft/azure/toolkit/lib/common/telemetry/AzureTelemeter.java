/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry.Properties;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry.Property;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AzureTelemeter {
    private static final String SERVICE_NAME = "serviceName";
    private static final String OPERATION_NAME = "operationName";
    private static final String OP_ID = "op_id";
    private static final String OP_NAME = "op_name";
    private static final String OP_TYPE = "op_type";
    private static final String OP_PARENT_ID = "op_parentId";

    private static final String ERROR_CODE = "error.code";
    private static final String ERROR_MSG = "error.message";
    private static final String ERROR_TYPE = "error.type";
    private static final String ERROR_CLASSNAME = "error.class_name";
    private static final String ERROR_STACKTRACE = "error.stack";
    @Getter
    @Setter
    @Nullable
    private static String eventNamePrefix;
    @Getter
    @Setter
    @Nullable
    private static AzureTelemetryClient client;

    @Nullable
    public static Map<String, String> getCommonProperties() {
        return Optional.ofNullable(client).map(AzureTelemetryClient::getDefaultProperties).orElse(null);
    }

    public static void setCommonProperties(@Nullable Map<String, String> commonProperties) {
        Optional.ofNullable(client).ifPresent(client -> client.setDefaultProperties(commonProperties));
    }

    public static void afterCreate(@Nonnull final IAzureOperation op) {
        final AzureTelemetry.Context context = AzureTelemetry.getContext(op);
        context.setCreateAt(Instant.now());
    }

    public static void beforeEnter(@Nonnull final IAzureOperation op) {
        final AzureTelemetry.Context context = AzureTelemetry.getContext(op);
        context.setEnterAt(Instant.now());
    }

    public static void afterExit(@Nonnull final IAzureOperation op) {
        final AzureTelemetry.Context context = AzureTelemetry.getContext(op);
        context.setExitAt(Instant.now());
        AzureTelemeter.log(AzureTelemetry.Type.INFO, serialize(op));
    }

    public static void onError(@Nonnull final IAzureOperation op, Throwable error) {
        final AzureTelemetry.Context context = AzureTelemetry.getContext(op);
        context.setExitAt(Instant.now());
        AzureTelemeter.log(AzureTelemetry.Type.ERROR, serialize(op), error);
    }

    public static void log(final AzureTelemetry.Type type, final Map<String, String> properties, final Throwable e) {
        if (Objects.nonNull(e)) {
            properties.putAll(serialize(e));
        }
        AzureTelemeter.log(type, properties);
    }

    public static void log(final AzureTelemetry.Type type, final Map<String, String> properties) {
        if (client != null) {
            properties.putAll(Optional.ofNullable(getCommonProperties()).orElse(new HashMap<>()));
            final String eventName = Optional.ofNullable(getEventNamePrefix()).orElse("AzurePlugin") + "/" + type.name();
            client.trackEvent(eventName, properties, null);
        }
    }

    @Nonnull
    private static Map<String, String> serialize(@Nonnull final IAzureOperation op) {
        final AzureTelemetry.Context context = AzureTelemetry.getContext(op);
        final Map<String, String> actionProperties = getActionProperties(context.getOperation());
        final Optional<IAzureOperation> parent = Optional.ofNullable(op.getParent());
        final Map<String, String> properties = new HashMap<>();
        final String name = op.getName().replaceAll("\\(.+\\)", "(***)"); // e.g. `appservice|file.list.dir`
        final String[] parts = name.split("\\."); // ["appservice|file", "list", "dir"]
        final String[] compositeServiceName = parts[0].split("\\|"); // ["appservice", "file"]
        final String mainServiceName = compositeServiceName[0]; // "appservice"
        final String operationName = compositeServiceName.length > 1 ? parts[1] + "_" + compositeServiceName[1] : parts[1]; // "list_file"
        properties.put(SERVICE_NAME, mainServiceName);
        properties.put(OPERATION_NAME, operationName);
        properties.put(OP_ID, op.getId());
        properties.put(OP_PARENT_ID, parent.map(IAzureOperation::getId).orElse("/"));
        properties.put(OP_NAME, name);
        properties.put(OP_TYPE, op.getType());
        properties.putAll(actionProperties);
        if (op instanceof AzureOperationRef) {
            properties.putAll(getParameterProperties((AzureOperationRef) op));
        }
        properties.putAll(context.getProperties());
        return properties;
    }

    private static Map<String, String> getParameterProperties(AzureOperationRef ref) {
        final HashMap<String, String> properties = new HashMap<>();
        final Object[] paramValues = ref.getParamValues();
        final Parameter[] parameters = ref.getMethod().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            final Parameter param = parameters[i];
            final Object value = paramValues[i];
            Optional.ofNullable(param.getAnnotation(Property.class))
                    .map(Property::value)
                    .map(n -> Property.PARAM_NAME.equals(n) ? param.getName() : n)
                    .ifPresent((name) -> properties.put(name, Optional.ofNullable(value).map(Object::toString).orElse("")));
            Optional.ofNullable(param.getAnnotation(Properties.class))
                    .map(Properties::value)
                    .map(AzureTelemeter::instantiate)
                    .map(converter -> converter.convert(value))
                    .ifPresent(properties::putAll);
        }
        return properties;
    }

    @Nonnull
    private static Map<String, String> getActionProperties(IAzureOperation operation) {
        return Optional.ofNullable(operation)
                .map(IAzureOperation::getActionParent)
                .map(o -> o.get(AzureTelemetry.Context.class, new AzureTelemetry.Context(o)))
                .map(AzureTelemetry.Context::getProperties)
                .orElse(new HashMap<>());
    }

    @SneakyThrows
    private static <U> U instantiate(Class<? extends U> clazz) {
        return clazz.newInstance();
    }

    @Nonnull
    private static HashMap<String, String> serialize(@Nonnull Throwable e) {
        final HashMap<String, String> properties = new HashMap<>();
        final ErrorType type = ErrorType.userError; // TODO: (@wangmi & @Hanxiao.Liu)decide error type based on the type of ex.
        properties.put(ERROR_CLASSNAME, e.getClass().getName());
        properties.put(ERROR_TYPE, type.name());
        properties.put(ERROR_MSG, e.getMessage());
        properties.put(ERROR_STACKTRACE, ExceptionUtils.getStackTrace(e));
        return properties;
    }

    private enum ErrorType {
        userError,
        systemError,
        serviceError,
        toolError,
        unclassifiedError
    }
}
