/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.common.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.microsoft.azure.tools.common.exception.CommandExecuteException;

public class CommandUtil {
    private static final boolean isWindows = System.getProperty("os.name").contains("Windows");

    public static String executeCommandAndGetOutput(final String cmd, File cwd)
            throws IOException, InterruptedException {
        final String[] cmds = new String[]{isWindows ? "cmd.exe" : "bash", "/c", cmd};
        final Process p = Runtime.getRuntime().exec(cmds, null, cwd);
        final int exitCode = p.waitFor();
        if (exitCode != 0) {
            String errorLog = IOUtils.toString(p.getErrorStream(), "utf8");
            throw new CommandExecuteException(String.format("Cannot execute '%s' due to error: %s", cmd, errorLog));
        }
        return IOUtils.toString(p.getInputStream(), "utf8");
    }
}
