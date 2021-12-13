/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

public class CommandExecuteException extends AzureToolkitRuntimeException {
    private static final long serialVersionUID = 4582230448665092548L;

    public CommandExecuteException(String message) {
        super(message);
    }
}
