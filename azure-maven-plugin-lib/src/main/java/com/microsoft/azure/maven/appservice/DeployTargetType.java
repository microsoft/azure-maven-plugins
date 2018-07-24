/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.appservice;

public enum DeployTargetType {
    WEBAPP("Web App"),

    SLOT("Deployment Slot"),

    FUNCTION("Function App");

    private final String value;

    DeployTargetType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

