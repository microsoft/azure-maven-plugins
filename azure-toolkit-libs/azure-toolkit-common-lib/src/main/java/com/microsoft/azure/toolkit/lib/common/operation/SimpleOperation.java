/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;

@RequiredArgsConstructor
public class SimpleOperation implements IAzureOperation {

    @Getter
    @Nonnull
    private final AzureString title;
    @Nonnull
    private final AzureOperation.Type type;
    @Getter
    @Setter
    private IAzureOperation parent;

    @Nonnull
    @Override
    public String getType() {
        return this.type.name();
    }
}
