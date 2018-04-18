/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

import com.microsoft.azure.maven.function.handlers.CommandHandler;
import com.microsoft.azure.maven.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.maven.function.utils.CommandUtils;

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

    public static final String FUNC_HOST_START_CMD = "func host start";

    //region Entry Point

    @Override
    protected void doExecute() throws Exception {
        final CommandHandler commandHandler = new CommandHandlerImpl(this.getLog());

        checkStageDirectoryExistence();

        checkRuntimeExistence(commandHandler);

        runFunctions(commandHandler);
    }

    protected void checkStageDirectoryExistence() throws Exception {
        final File file = new File(getDeploymentStageDirectory());
        if (!file.exists() || !file.isDirectory()) {
            throw new MojoExecutionException(STAGE_DIR_NOT_FOUND);
        }
        info(STAGE_DIR_FOUND + getDeploymentStageDirectory());
    }

    protected void checkRuntimeExistence(final CommandHandler handler) throws Exception {
        handler.runCommandWithReturnCodeCheck(
                getCheckRuntimeCommand(),
                false, /* showStdout */
                null, /* workingDirectory */
                CommandUtils.getDefaultValidReturnCodes(),
                RUNTIME_NOT_FOUND
        );
        info(RUNTIME_FOUND);
    }

    protected void runFunctions(final CommandHandler handler) throws Exception {
        info(START_RUN_FUNCTIONS);
        handler.runCommandWithReturnCodeCheck(
                getStartFunctionHostCommand(),
                true, /* showStdout */
                getDeploymentStageDirectory(),
                CommandUtils.getValidReturnCodes(),
                RUN_FUNCTIONS_FAILURE
        );
    }

    //endregion

    //region Build commands

    protected String getCheckRuntimeCommand() {
        return "func";
    }

    protected String getStartFunctionHostCommand() {
        return FUNC_HOST_START_CMD;
    }

    //endregion
}
