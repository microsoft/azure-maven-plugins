/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@CustomLog
public class OperationContext {
    private static final OperationContext NULL = new OperationContext(null);
    @Nullable
    private final Operation operation;
    @Setter
    private IAzureMessager messager = null;
    @Getter
    private final Map<String, Object> messageProperties = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, String> telemetryProperties = new ConcurrentHashMap<>();

    public OperationContext(@Nonnull Operation operation) {
        this.operation = operation;
    }

    public void setMessageProperty(String key, String val) {
        this.messageProperties.put(key, val);
    }

    public void setTelemetryProperty(String key, String val) {
        this.telemetryProperties.put(key, val);
    }

    public void setTelemetryProperties(Map<String, String> properties) {
        this.telemetryProperties.putAll(properties);
    }

    public String getProperty(String key) {
        return this.telemetryProperties.get(key);
    }

    @Nonnull
    public IAzureMessager getMessager() {
        if (Objects.isNull(this.messager)) {
            this.messager = Optional.ofNullable(getParent())
                .map(OperationContext::getMessager)
                .orElse(AzureMessager.getDefaultMessager());
        }
        return this.messager;
    }

    @Nonnull
    public OperationContext getAction() {
        return Optional.ofNullable(this.operation)
            .map(Operation::getActionParent)
            .map(Operation::getContext)
            .orElseGet(() -> getNull(this.operation));
    }

    @Nullable
    public OperationContext getParent() {
        return Optional.ofNullable(this.operation)
            .map(Operation::getParent)
            .map(Operation::getContext)
            .orElse(null);
    }

    @Nonnull
    public static OperationContext current() {
        final Operation current = Operation.current();
        return Optional.ofNullable(current).map(Operation::getContext).orElseGet(() -> OperationContext.getNull(current));
    }

    @Nonnull
    public static OperationContext action() {
        final Operation current = Operation.current();
        return Optional.of(OperationContext.current()).map(OperationContext::getAction).orElseGet(() -> OperationContext.getNull(current));
    }

    @Nonnull
    private static OperationContext getNull(@Nullable Operation operation) {
        final String op = Optional.ofNullable(operation).map(Operation::getId).orElse(null);
        log.warn(String.format("default to NULL OperationContext, because operation or its action operation is null:%s", op));
        return NULL;
    }
}
