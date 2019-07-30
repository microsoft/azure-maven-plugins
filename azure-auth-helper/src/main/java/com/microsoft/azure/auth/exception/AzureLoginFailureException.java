/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.exception;

public class AzureLoginFailureException extends Exception {
    private static final long serialVersionUID = -2454602745147535476L;

    public AzureLoginFailureException(String message) {
        super(message);
    }
}
