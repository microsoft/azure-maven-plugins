/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Run Azure Java Functions
 */
@Mojo(name = "run")
public class RunMojo extends AbstractFunctionMojo {
    public static final String STAGE_DIR_FOUND = "Azure Functions stage directory found at: ";
    public static final String STAGE_DIR_NOT_FOUND =
            "Stage directory not found. Please run mvn:package or azure-functions:package first.";
    public static final String RUNTIME_FOUND = "Azure Functions CLI 2.0 found.";
    public static final String RUNTIME_NOT_FOUND = "Azure Functions CLI 2.0 not found. " +
            "Please run 'npm i -g azure-functions-core-tools' to install Azure Functions CLI 2.0 first.";
    public static final String RUN_FUNCTIONS_FAILURE = "Failed to run Azure Functions. Please checkout console output.";
    public static final String START_RUN_FUNCTIONS = "Starting running Azure Functions...";
    public static final String STOP_RUN_FUNCTIONS = "Stopping running Azure Functions...";

    @Override
    protected void doExecute() throws Exception {
        checkStageDirectoryExistence();

        checkRuntimeExistence();

        runFunctions();
    }

    protected void checkStageDirectoryExistence() throws Exception {
        runCommand(getCheckStageDirectoryCommand(), false, getDefaultValidReturnCodes(), STAGE_DIR_NOT_FOUND);
        getLog().info(STAGE_DIR_FOUND + getDeploymentStageDirectory());
    }

    protected void checkRuntimeExistence() throws Exception {
        runCommand(getCheckRuntimeCommand(), false, getDefaultValidReturnCodes(), RUNTIME_NOT_FOUND);
        getLog().info(RUNTIME_FOUND);
    }

    protected void runFunctions() throws Exception {
        getLog().info(START_RUN_FUNCTIONS);
        runCommand(getRunFunctionsCommand(), true, getValidReturnCodes(), RUN_FUNCTIONS_FAILURE);
        getLog().info(STOP_RUN_FUNCTIONS);
    }

    protected String[] getCheckStageDirectoryCommand() {
        final String command = isWindows() ?
                String.format("cd /D %s", getDeploymentStageDirectory()) :
                String.format("cd %s", getDeploymentStageDirectory());
        return buildCommand(command);
    }

    protected String[] getCheckRuntimeCommand() {
        return buildCommand("func");
    }

    protected String[] getRunFunctionsCommand() {
        final String command = isWindows() ?
                String.format("cd /D %s && func host start", getDeploymentStageDirectory()) :
                String.format("cd %s; func host start", getDeploymentStageDirectory());
        return buildCommand(command);
    }

    protected String[] buildCommand(final String command) {
        return isWindows() ?
            new String[]{"cmd.exe", "/c", command} :
            new String[]{"sh", "-c", command};
    }

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
        getLog().debug("Executing command: " + StringUtils.join(command, " "));

        final ProcessBuilder.Redirect redirect = getStdoutRedirect(showStdout);
        final Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(redirect)
                .start();
        process.waitFor();

        handleExitValue(process.exitValue(), validReturnCodes, errorMessage, process.getInputStream());
    }

    protected ProcessBuilder.Redirect getStdoutRedirect(boolean showStdout) {
        return showStdout ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.PIPE;
    }

    protected void handleExitValue(int exitValue, final List<Long> validReturnCodes, final String errorMessage,
                                   final InputStream inputStream) throws Exception {
        getLog().debug("Process exit value: " + exitValue);
        if (!validReturnCodes.contains(Integer.toUnsignedLong(exitValue))) {
            // input stream is a merge of standard output and standard error of the sub-process
            showErrorIfAny(inputStream);
            getLog().error(errorMessage);
            throw new Exception(errorMessage);
        }
    }

    protected void showErrorIfAny(final InputStream inputStream) throws Exception {
        if (inputStream != null) {
            final String input = IOUtil.toString(inputStream);
            getLog().error(StringUtils.strip(input, "\n"));
        }
    }
}
