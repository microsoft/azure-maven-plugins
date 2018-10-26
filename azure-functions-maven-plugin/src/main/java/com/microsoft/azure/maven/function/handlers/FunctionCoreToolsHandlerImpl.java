/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.github.zafarkhaja.semver.Version;
import com.microsoft.azure.maven.function.AbstractFunctionMojo;
import com.microsoft.azure.maven.function.utils.CommandUtils;

public class FunctionCoreToolsHandlerImpl implements FunctionCoreToolsHandler {

    public static final String FUNC_EXTENSIONS_INSTALL_TEMPLATE = "func extensions install -c \"%s\"";
    public static final String INSTALL_FUNCTION_EXTENSIONS_FAIL = "Failed to install the Function extensions";
    public static final String OUTDATED_LOCAL_FUNCTION_CORE_TOOLS = "Local Azure Functions Core Tools does not " +
            "exist or it is too old to support extension auto-install, skip it in the package phase. To install or" +
            " upgrade it, see: https://aka.ms/azfunc-install";
    public static final String NEED_UPDATE_FUNCTION_CORE_TOOLS = "Local version of Azure Functions Core Tools (%s) " +
            "does not match the latest (%s). Please update it for the best experience. " +
            "See: https://aka.ms/azfunc-install";
    public static final String GET_LATEST_VERSION_CMD = "npm view azure-functions-core-tools dist-tags.core";
    public static final String GET_LATEST_VERSION_FAIL = "Failed to check update for Azure Functions Core Tools";
    public static final String GET_LOCAL_VERSION_CMD = "func --version";
    public static final String GET_LOCAL_VERSION_FAIL = "Failed to get Azure Functions Core Tools version locally";
    public static final Version LEAST_SUPPORTED_VERSION = Version.valueOf("2.0.1-beta.26");

    private AbstractFunctionMojo mojo;
    private CommandHandler commandHandler;

    public FunctionCoreToolsHandlerImpl(final AbstractFunctionMojo mojo, final CommandHandler commandHandler) {
        this.mojo = mojo;
        this.commandHandler = commandHandler;
    }

    @Override
    public void installExtension() throws Exception {
        if (checkLocalCoreToolsVersion()) {
            installFunctionExtension();
        } else {
            throw new Exception(OUTDATED_LOCAL_FUNCTION_CORE_TOOLS);
        }
    }

    protected void installFunctionExtension() throws Exception {
        commandHandler.runCommandWithReturnCodeCheck(
                String.format(FUNC_EXTENSIONS_INSTALL_TEMPLATE, getProjectBasePath()),
                true,
                this.mojo.getDeploymentStagingDirectoryPath(),
                CommandUtils.getDefaultValidReturnCodes(),
                INSTALL_FUNCTION_EXTENSIONS_FAIL
        );
    }

    protected boolean checkLocalCoreToolsVersion() {
        final String localVersion = getLocalFunctionCoreToolsVersion();
        final String latestCoreVersion = getLatestFunctionCoreToolsVersion();
        // Verify if local function core tools support auto install
        if (localVersion == null || LEAST_SUPPORTED_VERSION.greaterThan(Version.valueOf(localVersion))) {
            return false;
        } else {
            // Verify whether local function core tools is the latest version
            if (latestCoreVersion == null) {
                this.mojo.warning(GET_LATEST_VERSION_FAIL);
            } else {
                if (Version.valueOf(localVersion).lessThan(Version.valueOf(latestCoreVersion))) {
                    this.mojo.warning(String.format(NEED_UPDATE_FUNCTION_CORE_TOOLS, localVersion, latestCoreVersion));
                }
            }
        }
        return true;
    }

    protected String getLatestFunctionCoreToolsVersion() {
        try {
            final String latestCoreVersion = commandHandler.runCommandAndGetOutput(
                    GET_LATEST_VERSION_CMD,
                    false, /* showStdout */
                    null /* workingDirectory */
            );
            Version.valueOf(latestCoreVersion);
            return latestCoreVersion;
        } catch (Exception e) {
            this.mojo.getLog().warn(GET_LATEST_VERSION_FAIL);
            return null;
        }
    }

    protected String getLocalFunctionCoreToolsVersion() {
        try {
            final String localVersion = commandHandler.runCommandAndGetOutput(
                    GET_LOCAL_VERSION_CMD,
                    false, /* showStdout */
                    null /* workingDirectory */
            );
            Version.valueOf(localVersion);
            return localVersion;
        } catch (Exception e) {
            this.mojo.getLog().warn(GET_LOCAL_VERSION_FAIL);
            return null;
        }
    }

    protected String getProjectBasePath() {
        return this.mojo.getProject().getBasedir().getAbsolutePath();
    }
}
