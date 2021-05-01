/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;

import java.io.File;

public interface FunctionCoreToolsHandler {
    void installExtension(File stagingDirectory, File basedir) throws AzureExecutionException;
}
