/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.auth.exception;

public class AzureLoginException extends Exception {
    public AzureLoginException(String message) {
        super(message);
    }

    public AzureLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
