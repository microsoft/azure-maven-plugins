/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.function.handlers.CommandHandler;
import com.microsoft.azure.maven.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.maven.function.utils.CommandUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Run Azure Java Functions locally. Azure Functions Core Tools is required to be installed first.
 */
@Mojo(name = "run")
public class RunMojo extends AbstractFunctionMojo {
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
    protected void doExecute() throws Exception {
        final CommandHandler commandHandler = new CommandHandlerImpl(this.getLog());

        checkStageDirectoryExistence();

        checkRuntimeExistence(commandHandler);

        runFunctions(commandHandler);
    }

    protected void checkStageDirectoryExistence() throws Exception {
        final File file = new File(getDeploymentStagingDirectoryPath());
        if (!file.exists() || !file.isDirectory()) {
            throw new MojoExecutionException(STAGE_DIR_NOT_FOUND);
        }
        info(STAGE_DIR_FOUND + getDeploymentStagingDirectoryPath());
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
        handler.runCommandWithReturnCodeCheck(
                getStartFunctionHostCommand(),
                true, /* showStdout */
                getDeploymentStagingDirectoryPath(),
                CommandUtils.getValidReturnCodes(),
                RUN_FUNCTIONS_FAILURE
        );
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
