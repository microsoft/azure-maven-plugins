/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.configurations;

public enum FunctionExtensionVersion {

    VERSION_2(2, "~2"),
    VERSION_3(3, "~3"),
    VERSION_4(4, "~4"),
    BETA(Integer.MAX_VALUE, "beta"), // beta refers to the latest version
    UNKNOWN(Integer.MIN_VALUE, "unknown");

    private int value;
    private String version;

    FunctionExtensionVersion(int value, String version) {
        this.version = version;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getVersion() {
        return version;
    }
}
