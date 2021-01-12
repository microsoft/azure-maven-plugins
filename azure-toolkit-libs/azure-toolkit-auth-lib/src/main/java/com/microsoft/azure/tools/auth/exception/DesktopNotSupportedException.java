/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.exception;

public class DesktopNotSupportedException extends LoginFailureException {
    private static final long serialVersionUID = 6774808712719406687L;

    public DesktopNotSupportedException(String message) {
        super(message);
    }
}
