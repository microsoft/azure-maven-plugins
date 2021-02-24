/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

import lombok.Getter;

@Getter
public class AzureToolkitRuntimeException extends RuntimeException {
    private final String action;
    private final String actionId;

    public AzureToolkitRuntimeException(String error) {
        this(error, null, null);
    }

    public AzureToolkitRuntimeException(String error, Throwable cause) {
        this(error, cause, null);
    }

    public AzureToolkitRuntimeException(String error, String action) {
        this(error, null, action);
    }

    public AzureToolkitRuntimeException(String error, Throwable cause, String action) {
        this(error, cause, action, null);
    }

    public AzureToolkitRuntimeException(String error, Throwable cause, String action, String actionId) {
        super(error, cause);
        this.action = action;
        this.actionId = actionId;
    }
}
