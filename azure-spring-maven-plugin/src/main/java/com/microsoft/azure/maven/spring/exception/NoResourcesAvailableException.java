/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.exception;

public class NoResourcesAvailableException extends Exception {

    public NoResourcesAvailableException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -8631337506110108893L;

}
