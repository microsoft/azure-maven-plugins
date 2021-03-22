/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;

@Getter
public class AzureOperationException extends AzureToolkitRuntimeException {
    private final AzureOperationRef operation;

    AzureOperationException(final AzureOperationRef operation, final Throwable cause) {
        this(operation, cause, null);
    }

    AzureOperationException(final AzureOperationRef operation, final String action) {
        this(operation, null, action);
    }

    AzureOperationException(final AzureOperationRef operation, final Throwable cause, final String action) {
        this(operation, cause, action, null);
    }

    AzureOperationException(final AzureOperationRef operation, final Throwable cause, final String action, final String actionId) {
        super(operation.getTitle(), cause, action, actionId);
        this.operation = operation;
    }
}
