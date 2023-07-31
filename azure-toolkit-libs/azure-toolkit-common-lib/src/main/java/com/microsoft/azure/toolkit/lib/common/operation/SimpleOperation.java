/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
public class SimpleOperation extends OperationBase {
    @Nonnull
    private final AzureString title;
    @Getter
    @Nonnull
    private final Callable<?> body;

    @Override
    public String toString() {
        return String.format("{name:'%s', method:%s}", this.title.getName(), this.body.getClass().getName());
    }

    @Nonnull
    @Override
    public String getId() {
        return this.title.getName();
    }

    @Nullable
    @Override
    public AzureString getDescription() {
        return this.title;
    }
}
