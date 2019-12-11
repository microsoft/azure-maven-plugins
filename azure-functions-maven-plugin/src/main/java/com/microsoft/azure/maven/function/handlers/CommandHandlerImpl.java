/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.maven.function.utils.CommandUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

public class CommandHandlerImpl implements CommandHandler {

    private Log logger;

    public CommandHandlerImpl(final Log logger) {
        this.logger = logger;
    }

    @Override
    public void runCommandWithReturnCodeCheck(final String command,
                                              final boolean showStdout,
                                              final String workingDirectory,
                                              final List<Long> validReturnCodes,
                                              final String errorMessage) throws Exception {
        final Process process = runCommand(command, showStdout, workingDirectory);

        handleExitValue(process.exitValue(), validReturnCodes, errorMessage, process.getInputStream());
    }

    @Override
    public String runCommandAndGetOutput(final String command,
                                         final boolean showStdout,
                                         final String workingDirectory) throws Exception {
        final Process process = runCommand(command, showStdout, workingDirectory);

        return getOutputFromProcess(process);
    }

    protected String getOutputFromProcess(final Process process) throws IOException {
        try (final BufferedReader stdInput = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")))) {
            final StringBuffer stdout = new StringBuffer();
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                stdout.append(s);
            }
            return stdout.toString().trim();
        }
    }

    protected Process runCommand(final String command,
                                 final boolean showStdout,
                                 final String workingDirectory) throws Exception {
        this.logger.debug("Executing command: " + StringUtils.join(command, " "));

        final ProcessBuilder.Redirect redirect = getStdoutRedirect(showStdout);
        final ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(command))
                .redirectOutput(redirect)
                .redirectErrorStream(true);

        if (workingDirectory != null) {
            processBuilder.directory(new File(workingDirectory));
        }

        final Process process = processBuilder.start();

        process.waitFor();

        return process;
    }

    protected static String[] buildCommand(final String command) {
        return CommandUtils.isWindows() ?
                new String[]{"cmd.exe", "/c", command} :
                new String[]{"sh", "-c", command};
    }

    protected static ProcessBuilder.Redirect getStdoutRedirect(boolean showStdout) {
        return showStdout ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.PIPE;
    }

    protected void handleExitValue(int exitValue,
                                   final List<Long> validReturnCodes,
                                   final String errorMessage,
                                   final InputStream inputStream) throws Exception {
        this.logger.debug("Process exit value: " + exitValue);
        if (!validReturnCodes.contains(Integer.toUnsignedLong(exitValue))) {
            // input stream is a merge of standard output and standard error of the sub-process
            showErrorIfAny(inputStream);
            this.logger.error(errorMessage);
            throw new Exception(errorMessage);
        }
    }

    protected void showErrorIfAny(final InputStream inputStream) throws Exception {
        if (inputStream != null) {
            final String input = IOUtil.toString(inputStream);
            this.logger.error(org.apache.commons.lang3.StringUtils.strip(input, "\n"));
        }
    }
}
