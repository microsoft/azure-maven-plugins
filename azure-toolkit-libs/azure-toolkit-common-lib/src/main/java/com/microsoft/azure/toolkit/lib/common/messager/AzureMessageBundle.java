/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureBundle;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;

public class AzureMessageBundle {
    private static final String BUNDLE = "bundles.com.microsoft.azure.toolkit.message";

    private static final AzureBundle bundle = new AzureBundle(BUNDLE);

    public static AzureString message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String name, @Nonnull Object... params) {
        return AzureString.format(bundle, name, params);
    }
}
