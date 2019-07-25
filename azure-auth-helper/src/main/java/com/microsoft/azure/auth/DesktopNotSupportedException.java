/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;
public class DesktopNotSupportedException extends Exception {
    private static final long serialVersionUID = -5613873966928201379L;

    public DesktopNotSupportedException(String message) {
        super(message);
    }
}
