/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.handlers.CommandHandler;
import com.microsoft.azure.common.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.common.function.utils.CommandUtils;
import com.microsoft.azure.common.logging.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Run Azure Java Functions locally. Azure Functions Core Tools is required to be installed first.
 */
@Mojo(name = "run")
public class RunMojo extends AbstractFunctionMojo {
    protected static final String FUNC_CMD = "func";
    protected static final String FUNC_HOST_START_CMD = "func host start";
    protected static final String RUN_FUNCTIONS_FAILURE = "Failed to run Azure Functions. Please checkout console output.";
    protected static final String RUNTIME_NOT_FOUND = "Azure Functions Core Tools not found. " +
            "Please go to https://aka.ms/azfunc-install to install Azure Functions Core Tools first.";
    private static final String STAGE_DIR_FOUND = "Azure Function App's staging directory found at: ";
    private static final String STAGE_DIR_NOT_FOUND =
            "Stage directory not found. Please run mvn package first.";
    private static final String RUNTIME_FOUND = "Azure Functions Core Tools found.";
    private static final String FUNC_HOST_START_WITH_DEBUG_CMD = "func host start --language-worker -- " +
            "\"-agentlib:jdwp=%s\"";
    private static final ComparableVersion JAVA_9 = new ComparableVersion("9");
    private static final ComparableVersion FUNC_3 = new ComparableVersion("3");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION = new ComparableVersion("3.0.2630");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION_V2 = new ComparableVersion("2.7.2628");
    private static final String FUNC_VERSION_CMD = "func -v";
    private static final String FUNCTION_CORE_TOOLS_OUT_OF_DATE = "Local function core tools didn't support java 9 or higher runtime, " +
            "to update it, see: https://aka.ms/azfunc-install.";

    /**
     * Config String for local debug
     *
     * @since 1.0.0-beta-7
     */
    @Parameter(property = "localDebugConfig", defaultValue = "transport=dt_socket,server=y,suspend=n,address=5005")
    protected String localDebugConfig;

    //region Getter

    public String getLocalDebugConfig() {
        return localDebugConfig;
    }

    public void setLocalDebugConfig(String localDebugConfig) {
        this.localDebugConfig = localDebugConfig;
    }

    //endregion

    //region Entry Point

    @Override
    protected void doExecute() throws AzureExecutionException {
        final CommandHandler commandHandler = new CommandHandlerImpl();

        checkStageDirectoryExistence();

        checkRuntimeExistence(commandHandler);

        checkRuntimeCompatibility(commandHandler);

        runFunctions(commandHandler);
    }

    protected void checkStageDirectoryExistence() throws AzureExecutionException {
        final File file = new File(getDeploymentStagingDirectoryPath());
        if (!file.exists() || !file.isDirectory()) {
            throw new AzureExecutionException(STAGE_DIR_NOT_FOUND);
        }
        Log.info(STAGE_DIR_FOUND + getDeploymentStagingDirectoryPath());
    }

    protected void checkRuntimeExistence(final CommandHandler handler) throws AzureExecutionException {
        handler.runCommandWithReturnCodeCheck(
                getCheckRuntimeCommand(),
                true, /* showStdout */
                null, /* workingDirectory */
                CommandUtils.getDefaultValidReturnCodes(),
                RUNTIME_NOT_FOUND
        );
        Log.info(RUNTIME_FOUND);
    }

    protected void runFunctions(final CommandHandler handler) throws AzureExecutionException {
        handler.runCommandWithReturnCodeCheck(
                getStartFunctionHostCommand(),
                true, /* showStdout */
                getDeploymentStagingDirectoryPath(),
                CommandUtils.getValidReturnCodes(),
                RUN_FUNCTIONS_FAILURE
        );
    }

    private void checkRuntimeCompatibility(final CommandHandler handler) throws AzureExecutionException {
        // Maven will always refer JAVA_HOME, which is also adopted by function core tools
        // So we could get function core tools runtime by java.version
        final ComparableVersion javaVersion = new ComparableVersion(System.getProperty("java.version"));
        if (javaVersion.compareTo(JAVA_9) < 0) {
            // No need to check within java 8 or lower
            return;
        }
        final ComparableVersion funcVersion = new ComparableVersion(handler.runCommandAndGetOutput(FUNC_VERSION_CMD, false, null));
        final ComparableVersion minimumVersion = funcVersion.compareTo(FUNC_3) >= 0 ? MINIMUM_JAVA_9_SUPPORTED_VERSION :
                MINIMUM_JAVA_9_SUPPORTED_VERSION_V2;
        if (funcVersion.compareTo(minimumVersion) < 0) {
            throw new AzureExecutionException(FUNCTION_CORE_TOOLS_OUT_OF_DATE);
        }
    }

    //endregion

    //region Build commands

    protected String getCheckRuntimeCommand() {
        return FUNC_CMD;
    }

    protected String getStartFunctionHostCommand() {
        final String enableDebug = System.getProperty("enableDebug");
        if (StringUtils.isNotEmpty(enableDebug) && enableDebug.equalsIgnoreCase("true")) {
            return getStartFunctionHostWithDebugCommand();
        } else {
            return FUNC_HOST_START_CMD;
        }
    }

    protected String getStartFunctionHostWithDebugCommand() {
        return String.format(FUNC_HOST_START_WITH_DEBUG_CMD, this.getLocalDebugConfig());
    }

    //endregion
}
