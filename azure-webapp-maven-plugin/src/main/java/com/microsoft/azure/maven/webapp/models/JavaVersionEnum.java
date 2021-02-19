/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.models;

public enum JavaVersionEnum {
    JAVA_7("Java 7"),
    JAVA_8("Java 8"),
    JAVA_11("Java 11");
    private final String displayName;

    JavaVersionEnum(String displayName) {
        this.displayName = displayName;
    }

    public String toString() {
        return displayName;
    }

}
