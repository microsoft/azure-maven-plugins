/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

public class AzureAuthFailureException extends Exception {
    public AzureAuthFailureException(String message) {
        super(message);
    }
}
