/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public enum StorageAuthenticationMethod {
    SystemAssignedIdentity,
    UserAssignedIdentity,
    StorageAccountConnectionString;

    @Nullable
    public static StorageAuthenticationMethod fromString(@Nonnull final String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return Arrays.stream(values())
            .filter(method -> StringUtils.equalsIgnoreCase(method.name(), value))
            .findFirst().orElse(null);
    }
}
