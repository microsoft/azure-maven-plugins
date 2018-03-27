/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

/**
 * Run Azure Java Functions locally. Azure Functions Core Tools is required to be installed first.
 */
@Mojo(name = "run")
public class RunMojo extends AbstractFunctionMojo {
    public static final String STAGE_DIR_FOUND = "Azure Functions stage directory found at: ";
    public static final String STAGE_DIR_NOT_FOUND =
            "Stage directory not found. Please run mvn:package or azure-functions:package first.";
    public static final String RUNTIME_FOUND = "Azure Functions Core Tools found.";
    public static final String RUNTIME_NOT_FOUND = "Azure Functions Core Tools not found. " +
            "Please run 'npm i -g azure-functions-core-tools@core' to install Azure Functions Core Tools first.";
    public static final String RUN_FUNCTIONS_FAILURE = "Failed to run Azure Functions. Please checkout console output.";
    public static final String START_RUN_FUNCTIONS = "Starting running Azure Functions...";

    public static final String WINDOWS_HOST_START = "cd /D %s && func host start";
    public static final String LINUX_HOST_START = "cd %s; func host start";

    //region Entry Point

    @Override
    protected void doExecute() throws Exception {
        checkStageDirectoryExistence();

        checkRuntimeExistence();

        runFunctions();
    }

    protected void checkStageDirectoryExistence() throws Exception {
        runCommand(getCheckStageDirectoryCommand(), false, getDefaultValidReturnCodes(), STAGE_DIR_NOT_FOUND);
        info(STAGE_DIR_FOUND + getDeploymentStageDirectory());
    }

    protected void checkRuntimeExistence() throws Exception {
        runCommand(getCheckRuntimeCommand(), false, getDefaultValidReturnCodes(), RUNTIME_NOT_FOUND);
        info(RUNTIME_FOUND);
    }

    protected void runFunctions() throws Exception {
        info(START_RUN_FUNCTIONS);
        runCommand(getStartFunctionHostCommand(), true, getValidReturnCodes(), RUN_FUNCTIONS_FAILURE);
    }

    //endregion

    //region Build commands

    protected String[] getCheckStageDirectoryCommand() {
        final String command = format(isWindows() ? "cd /D %s" : "cd %s", getDeploymentStageDirectory());
        return buildCommand(command);
    }

    protected String[] getCheckRuntimeCommand() {
        return buildCommand("func");
    }

    protected String[] getStartFunctionHostCommand() {
        final String command = format(getStartFunctionHostTemplate(), getDeploymentStageDirectory());
        return buildCommand(command);
    }

    protected String getStartFunctionHostTemplate() {
        return isWindows() ? WINDOWS_HOST_START : LINUX_HOST_START;
    }

    protected String[] buildCommand(final String command) {
        return isWindows() ?
                new String[]{"cmd.exe", "/c", command} :
                new String[]{"sh", "-c", command};
    }

    //endregion

    //region Helper methods

    protected boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    protected List<Long> getDefaultValidReturnCodes() {
        return Arrays.asList(0L);
    }

    protected List<Long> getValidReturnCodes() {
        return isWindows() ?
                // Windows return code of CTRL-C is 3221225786
                Arrays.asList(0L, 3221225786L) :
                // Linux return code of CTRL-C is 130
                Arrays.asList(0L, 130L);
    }

    protected void runCommand(final String[] command, final boolean showStdout, final List<Long> validReturnCodes,
                              final String errorMessage) throws Exception {
        debug("Executing command: " + StringUtils.join(command, " "));

        final Redirect redirect = getStdoutRedirect(showStdout);
        final Process process = new ProcessBuilder(command)
                .redirectOutput(redirect)
                .redirectErrorStream(true)
                .start();

        process.waitFor();

        handleExitValue(process.exitValue(), validReturnCodes, errorMessage, process.getInputStream());
    }

    protected Redirect getStdoutRedirect(boolean showStdout) {
        return showStdout ? Redirect.INHERIT : Redirect.PIPE;
    }

    protected void handleExitValue(int exitValue, final List<Long> validReturnCodes, final String errorMessage,
                                   final InputStream inputStream) throws Exception {
        debug("Process exit value: " + exitValue);
        if (!validReturnCodes.contains(Integer.toUnsignedLong(exitValue))) {
            // input stream is a merge of standard output and standard error of the sub-process
            showErrorIfAny(inputStream);
            error(errorMessage);
            throw new Exception(errorMessage);
        }
    }

    protected void showErrorIfAny(final InputStream inputStream) throws Exception {
        if (inputStream != null) {
            final String input = IOUtil.toString(inputStream);
            error(StringUtils.strip(input, "\n"));
        }
    }

    //endregion
}
