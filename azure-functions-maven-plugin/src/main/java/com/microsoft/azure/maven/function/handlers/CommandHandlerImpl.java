/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.maven.function.utils.CommandUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

public class CommandHandlerImpl implements CommandHandler {
    @Override
    public void runCommandWithReturnCodeCheck(final String command,
                                              final boolean showStdout,
                                              final String workingDirectory,
                                              final List<Long> validReturnCodes,
                                              final String errorMessage) throws AzureExecutionException {
        try {
            final Process process = runCommand(command, showStdout, workingDirectory);

            handleExitValue(process.exitValue(), validReturnCodes, errorMessage, process.getInputStream());
        } catch (IOException | InterruptedException ex) {
            throw new AzureExecutionException("Cannot execute '" + command + "'", ex);
        }
    }

    @Override
    public String runCommandAndGetOutput(final String command,
                                         final boolean showStdout,
                                         final String workingDirectory) throws AzureExecutionException {
        try {
            final Process process = runCommand(command, showStdout, workingDirectory);
            return getOutputFromProcess(process);
        } catch (IOException | InterruptedException ex) {
            throw new AzureExecutionException("Cannot execute '" + command + "'", ex);
        }
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
                                 final String workingDirectory) throws IOException, InterruptedException {
        Log.debug("Executing command: " + StringUtils.join(command, " "));

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
                                   final InputStream inputStream) throws AzureExecutionException, IOException {
        Log.debug("Process exit value: " + exitValue);
        if (!validReturnCodes.contains(Integer.toUnsignedLong(exitValue))) {
            // input stream is a merge of standard output and standard error of the sub-process
            showErrorIfAny(inputStream);
            Log.error(errorMessage);
            throw new AzureExecutionException(errorMessage);
        }
    }

    protected void showErrorIfAny(final InputStream inputStream) throws IOException {
        if (inputStream != null) {
            final String input = IOUtils.toString(inputStream, "utf8");
            Log.error(StringUtils.strip(input, "\n"));
        }
    }
}
