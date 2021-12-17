/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;

import java.util.Optional;

@Getter
public class AzureOperationException extends AzureToolkitRuntimeException {
    private final IAzureOperation operation;

    public AzureOperationException(final IAzureOperation operation, final Throwable cause) {
        super(cause);
        this.operation = operation;
    }

    @Override
    public String getMessage() {
        return Optional.ofNullable(operation.getTitle()).map(Object::toString).orElse(null);
    }
}
