/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.event;

import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@Setter
@RequiredArgsConstructor
public class AzureOperationEvent<T extends AzureOperationEvent.Source<T>> implements AzureEvent<T> {
    private final T source;
    private final AzureOperationRef operation;
    private final Stage stage;

    @Nonnull
    @Override
    public String getType() {
        return operation.getName();
    }

    @Nullable
    @Override
    public T getPayload() {
        return source;
    }

    public interface Source<T> {
        @Nonnull
        default Source<T> getEventSource() {
            return this;
        }
    }

    public enum Stage {
        BEFORE, AFTER, ERROR
    }
}
