/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

public enum AccessTier {
    HOT("Hot"),
    COOL("Cool");
    private final String value;

    AccessTier(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }
}
