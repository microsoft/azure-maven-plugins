/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.exception;

public class MavenDecryptException extends Exception {
    private static final long serialVersionUID = 5207024853556212112L;

    public MavenDecryptException(String message) {
        super(message);
    }
}
