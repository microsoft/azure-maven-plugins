/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.Callable;

public interface Operation {
    String UNKNOWN_NAME = "<unknown>.<unknown>";

    @Nonnull
    String getExecutionId();

    @Nonnull
    String getId();

    Callable<?> getBody();

    @Nullable
    Object getSource();

    @Nonnull
    String getType();

    @Nonnull
    String getServiceName();

    @Nonnull
    String getOperationName();

    @Nullable
    AzureString getDescription();

    void setParent(Operation operation);

    @Nullable
    Operation getParent();

    OperationContext getContext();

    @Nullable
    default Operation getEffectiveParent() {
        final Operation parent = this.getParent();
        if (parent == null) {
            return null;
        } else if (!parent.getId().equals(UNKNOWN_NAME)) {
            return parent;
        } else {
            return parent.getEffectiveParent();
        }
    }

    @Nullable
    default Operation getActionParent() {
        if (StringUtils.equalsAnyIgnoreCase(this.getType(), Type.USER, Type.PLATFORM)) {
            return this;
        }
        return Optional.ofNullable(this.getParent()).map(Operation::getActionParent).orElse(null);
    }

    @Nullable
    static Operation current() {
        return OperationThreadContext.current().currentOperation();
    }

    @SneakyThrows
    static void execute(@Nonnull final AzureString title, @Nonnull final Callable<?> body, @Nullable final Object source) {
        final SimpleOperation operation = new SimpleOperation(title, body, source);
        AzureOperationAspect.execute(operation);
    }

    @SneakyThrows
    static void execute(@Nonnull final AzureString title, @Nonnull final Runnable body, @Nullable final Object source) {
        final SimpleOperation operation = new SimpleOperation(title, () -> {
            body.run();
            //noinspection ReturnOfNull
            return null;
        }, source);
        AzureOperationAspect.execute(operation);
    }

    interface Type {
        String USER = "user";
        String PLATFORM = "platform";
        String INTERNAL = "internal";
        String AZURE = "azure";
        String BOUNDARY = "boundary";
        String UNKNOWN = "unknown";
    }
}
