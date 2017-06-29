/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

public class OperationResult {
    private boolean isSuccess;

    private String message;

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getMessage() {
        return message;
    }

    public OperationResult(final boolean isSuccess, final String message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }
}
