/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.exceptions;

public class AzureExecutionException extends Exception {
    public AzureExecutionException(String errorMessage, Throwable err) {
        super(errorMessage.toString(), err);
    }

    public AzureExecutionException(String errorMessage) {
        super(errorMessage);
    }
}
