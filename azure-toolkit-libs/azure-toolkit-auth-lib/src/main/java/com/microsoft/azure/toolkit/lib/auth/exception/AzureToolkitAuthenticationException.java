/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.exception;

import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;

public class AzureToolkitAuthenticationException extends AzureToolkitRuntimeException {
    public AzureToolkitAuthenticationException(String error) {
        super(error, Action.AUTHENTICATE);
    }

    public AzureToolkitAuthenticationException(String error, Throwable cause) {
        super(error, cause, Action.AUTHENTICATE);
    }
}
