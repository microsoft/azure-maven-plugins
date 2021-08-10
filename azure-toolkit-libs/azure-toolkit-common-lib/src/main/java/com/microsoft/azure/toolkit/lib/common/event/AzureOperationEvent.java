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

@Getter
@Setter
@RequiredArgsConstructor
public class AzureOperationEvent<T extends AzureOperationEvent.Source<T>> implements AzureEvent<Object> {
    private final T source;
    private final AzureOperationRef operation;
    private final Stage stage;
    private Object payload;

    @Nonnull
    @Override
    public String getType() {
        return operation.getName();
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
