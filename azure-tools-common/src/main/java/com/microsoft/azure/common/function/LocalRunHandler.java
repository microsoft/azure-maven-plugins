/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.function;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.maven.function.handlers.CommandHandler;
import com.microsoft.azure.maven.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.maven.function.utils.CommandUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Run Azure Java Functions locally. Azure Functions Core Tools is required to be installed first.
 */
public class LocalRunHandler {
    public static final String STAGE_DIR_FOUND = "Azure Function App's staging directory found at: ";
    public static final String STAGE_DIR_NOT_FOUND =
            "Stage directory not found. Please run mvn package first.";
    public static final String RUNTIME_FOUND = "Azure Functions Core Tools found.";
    public static final String RUNTIME_NOT_FOUND = "Azure Functions Core Tools not found. " +
            "Please go to https://aka.ms/azfunc-install to install Azure Functions Core Tools first.";
    public static final String RUN_FUNCTIONS_FAILURE = "Failed to run Azure Functions. Please checkout console output.";

    public static final String FUNC_HOST_START_CMD = "func host start";
    public static final String FUNC_HOST_START_WITH_DEBUG_CMD = "func host start --language-worker -- " +
            "\"-agentlib:jdwp=%s\"";
    public static final String FUNC_CMD = "func";

    private String deploymentStagingDirectoryPath;
    private String localDebugConfig;

	public LocalRunHandler(String deploymentStagingDirectoryPath, String localDebugConfig) {
		this.deploymentStagingDirectoryPath = deploymentStagingDirectoryPath;
		this.localDebugConfig = localDebugConfig;
	}

    public void execute() throws AzureExecutionException {
        final CommandHandler commandHandler = new CommandHandlerImpl();

        checkStageDirectoryExistence();

        checkRuntimeExistence(commandHandler);

        runFunctions(commandHandler);
    }

    private void checkStageDirectoryExistence() throws AzureExecutionException {
        final File file = new File(deploymentStagingDirectoryPath);
        if (!file.exists() || !file.isDirectory()) {
            throw new AzureExecutionException(STAGE_DIR_NOT_FOUND);
        }
        Log.info(STAGE_DIR_FOUND + deploymentStagingDirectoryPath);
    }

    private void checkRuntimeExistence(final CommandHandler handler) throws AzureExecutionException {
        handler.runCommandWithReturnCodeCheck(
                getCheckRuntimeCommand(),
                false, /* showStdout */
                null, /* workingDirectory */
                CommandUtils.getDefaultValidReturnCodes(),
                RUNTIME_NOT_FOUND
        );
        Log.info(RUNTIME_FOUND);
    }

    private void runFunctions(final CommandHandler handler) throws AzureExecutionException {
        handler.runCommandWithReturnCodeCheck(
                getStartFunctionHostCommand(),
                true, /* showStdout */
                deploymentStagingDirectoryPath,
                CommandUtils.getValidReturnCodes(),
                RUN_FUNCTIONS_FAILURE
        );
    }

    private String getCheckRuntimeCommand() {
        return FUNC_CMD;
    }

    private String getStartFunctionHostCommand() {
        if (StringUtils.isNotEmpty(localDebugConfig)) {
            return getStartFunctionHostWithDebugCommand();
        } else {
            return FUNC_HOST_START_CMD;
        }
    }

    private String getStartFunctionHostWithDebugCommand() {
        return String.format(FUNC_HOST_START_WITH_DEBUG_CMD, this.localDebugConfig);
    }
}
