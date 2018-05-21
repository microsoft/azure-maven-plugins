/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import java.util.List;

public interface CommandHandler {

    void runCommandWithReturnCodeCheck(final String command,
                                       final boolean showStdout,
                                       final String workingDirectory,
                                       final List<Long> validReturnCodes,
                                       final String errorMessage) throws Exception;

    String runCommandAndGetOutput(final String command,
                                  final boolean showStdout,
                                  final String workingDirectory) throws Exception;
}
