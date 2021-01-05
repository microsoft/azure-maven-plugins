/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.exception;

public class LoginFailureException extends AzureLoginException {
    private static final long serialVersionUID = -94207672112213624L;

    public LoginFailureException(String message) {
        super(message);
    }

}
