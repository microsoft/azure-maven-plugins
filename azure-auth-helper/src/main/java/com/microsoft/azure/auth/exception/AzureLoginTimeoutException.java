/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.exception;

public class AzureLoginTimeoutException extends AzureLoginFailureException {

    /**
     * @param message
     */
    public AzureLoginTimeoutException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -3955832869411741263L;
}
