/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

import lombok.Getter;

@Getter
public class AzureToolkitException extends Exception {
    private final String action;
    private final String actionId;

    public AzureToolkitException(String error) {
        this(error, null, null);
    }

    public AzureToolkitException(String error, Throwable cause) {
        this(error, cause, null);
    }

    public AzureToolkitException(String error, String action) {
        this(error, null, action);
    }

    public AzureToolkitException(String error, Throwable cause, String action) {
        this(error, cause, action, null);
    }

    public AzureToolkitException(String message, Throwable cause, String action, String actionId) {
        super(message, cause);
        this.action = action;
        this.actionId = actionId;
    }
}
