/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.microsoft.azure.toolkit.lib.common.operation.MethodOperation;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry.Properties;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry.Property;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AzureTelemeter {
    public static final String SERVICE_NAME = "serviceName";
    public static final String OPERATION_NAME = "operationName";
    public static final String OP_ID = "op_id";
    public static final String OP_NAME = "op_name";
    public static final String OP_TYPE = "op_type";
    public static final String OP_PARENT_ID = "op_parentId";

    public static final String ERROR_CODE = "error.error_code";
    public static final String ERROR_MSG = "error.error_message";
    public static final String ERROR_TYPE = "error.error_type";
    public static final String ERROR_CLASSNAME = "error.error_class_name";
    public static final String ERROR_STACKTRACE = "error.error_stack";
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

    public static void afterCreate(@Nonnull final Operation op) {
        op.getContext().setTelemetryProperty(AzureTelemetry.OP_CREATE_AT, Instant.now().toString());
    }

    public static void beforeEnter(@Nonnull final Operation op) {
        op.getContext().setTelemetryProperty(AzureTelemetry.OP_ENTER_AT, Instant.now().toString());
    }

    public static void afterExit(@Nonnull final Operation op) {
        op.getContext().setTelemetryProperty(AzureTelemetry.OP_EXIT_AT, Instant.now().toString());
        AzureTelemeter.log(AzureTelemetry.Type.OP_END, serialize(op));
    }

    public static void onError(@Nonnull final Operation op, Throwable error) {
        op.getContext().setTelemetryProperty(AzureTelemetry.OP_EXIT_AT, Instant.now().toString());
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
    private static Map<String, String> serialize(@Nonnull final Operation op) {
        final OperationContext context = op.getContext();
        final Map<String, String> actionProperties = getActionProperties(op);
        final Optional<Operation> parent = Optional.ofNullable(op.getParent());
        final Map<String, String> properties = new HashMap<>();
        final String name = op.getId().replaceAll("\\(.+\\)", "(***)"); // e.g. `appservice.list_file.dir`
        final String[] parts = name.split("\\."); // ["appservice|file", "list", "dir"]
        if (parts.length > 1) {
            final String[] compositeServiceName = parts[0].split("\\|"); // ["appservice", "file"]
            final String mainServiceName = compositeServiceName[0]; // "appservice"
            final String operationName = compositeServiceName.length > 1 ? parts[1] + "_" + compositeServiceName[1] : parts[1]; // "list_file"
            properties.put(SERVICE_NAME, mainServiceName);
            properties.put(OPERATION_NAME, operationName);
        }
        properties.put(OP_ID, op.getExecutionId());
        properties.put(OP_PARENT_ID, parent.map(Operation::getExecutionId).orElse("/"));
        properties.put(OP_NAME, name);
        properties.put(OP_TYPE, op.getType());
        properties.putAll(actionProperties);
        if (op instanceof MethodOperation) {
            properties.putAll(getParameterProperties((MethodOperation) op));
        }
        properties.putAll(context.getTelemetryProperties());
        return properties;
    }

    private static Map<String, String> getParameterProperties(MethodOperation ref) {
        final HashMap<String, String> properties = new HashMap<>();
        final List<Triple<String, Parameter, Object>> args = ref.getInvocation().getArgs();
        for (final Triple<String, Parameter, Object> arg : args) {
            final Parameter param = arg.getMiddle();
            final Object value = arg.getRight();
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
    private static Map<String, String> getActionProperties(@Nonnull Operation operation) {
        return Optional.ofNullable(operation.getActionParent())
            .map(Operation::getContext)
            .map(OperationContext::getTelemetryProperties)
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
