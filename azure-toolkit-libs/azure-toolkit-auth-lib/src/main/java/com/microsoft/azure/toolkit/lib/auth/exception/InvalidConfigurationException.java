/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.exception;

public class InvalidConfigurationException extends AzureLoginException {
    public InvalidConfigurationException(String message) {
        super(message);
    }
}
