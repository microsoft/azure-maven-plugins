/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureBundle;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class AzureOperationBundle {
    private static final String BUNDLE = "bundles.com.microsoft.azure.toolkit.operation";

    private static final AzureBundle bundle = new AzureBundle(BUNDLE);

    public static AzureString title(@NotNull @PropertyKey(resourceBundle = BUNDLE) String name, @NotNull Object... params) {
        return AzureString.format(bundle, name, params);
    }
}
