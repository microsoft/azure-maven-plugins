/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.configurations;

public enum FunctionExtensionVersion {

    VERSION_2(2, "~2"),
    VERSION_3(3, "~3"),
    BETA(Integer.MAX_VALUE, "beta"); // beta refers to latest version

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
