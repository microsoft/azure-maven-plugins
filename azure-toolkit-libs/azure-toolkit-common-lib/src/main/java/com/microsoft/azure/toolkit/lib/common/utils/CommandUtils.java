/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * copied from: https://github.com/microsoft/azure-tools-for-java/blob/
 * 0efe5e430305369e17dadc214c256cbb4a906372/Utils/azuretools-core/src/com/microsoft/azuretools/utils/CommandUtils.java
 */
@Slf4j
public class CommandUtils {
    private static final String WINDOWS_STARTER = "cmd.exe";
    private static final String LINUX_MAC_STARTER = "/bin/sh";
    private static final String WINDOWS_SWITCHER = "/c";
    private static final String LINUX_MAC_SWITCHER = "-c";
    private static final String DEFAULT_WINDOWS_SYSTEM_ROOT = System.getenv("SystemRoot");
    private static final String DEFAULT_MAC_LINUX_PATH = "/bin/";

    public static String exec(final String commandWithArgs) throws IOException {
        final String starter = isWindows() ? WINDOWS_STARTER : LINUX_MAC_STARTER;
        final String switcher = isWindows() ? WINDOWS_SWITCHER : LINUX_MAC_SWITCHER;
        final String workingDirectory = getSafeWorkingDirectory();
        if (StringUtils.isEmpty(workingDirectory)) {
            final IllegalStateException exception = new IllegalStateException("A Safe Working directory could not be found to execute command from.");
            log.error(CommandUtils.class.getName(), "exec", exception);
            throw exception;
        }
        final String commandWithPath = isWindows() ? commandWithArgs : String.format("export PATH=$PATH:/usr/local/bin ; %s", commandWithArgs);
        return executeCommandAndGetOutput(starter, switcher, commandWithPath, new File(workingDirectory));
    }

    private static String executeCommandAndGetOutput(final String starter, final String switcher, final String commandWithArgs,
                                                    final File directory) throws IOException {
        final CommandLine commandLine = new CommandLine(starter);
        commandLine.addArgument(switcher, false);
        commandLine.addArgument(commandWithArgs, false);
        return executeCommandAndGetOutput(commandLine, directory);
    }

    private static String executeCommandAndGetOutput(final CommandLine commandLine, final File directory) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final PumpStreamHandler streamHandler = new PumpStreamHandler(out, err);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(directory);
        executor.setStreamHandler(streamHandler);
        executor.setExitValues(new int[] {0});
        try {
            executor.execute(commandLine);
            if (err.size() > 0) {
                log.warn(err.toString());
            }
            return out.toString();
        } finally {
            out.close();
            err.close();
        }
    }

    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    private static String getSafeWorkingDirectory() {
        if (isWindows()) {
            if (StringUtils.isEmpty(DEFAULT_WINDOWS_SYSTEM_ROOT)) {
                return null;
            }
            return DEFAULT_WINDOWS_SYSTEM_ROOT + "\\system32";
        } else {
            return DEFAULT_MAC_LINUX_PATH;
        }
    }
}
