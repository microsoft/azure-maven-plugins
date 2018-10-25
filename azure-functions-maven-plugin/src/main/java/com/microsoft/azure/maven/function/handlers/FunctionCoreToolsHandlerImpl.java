/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.github.zafarkhaja.semver.Version;
import com.microsoft.azure.maven.function.AbstractFunctionMojo;
import com.microsoft.azure.maven.function.bindings.HttpBinding;
import com.microsoft.azure.maven.function.utils.CommandUtils;

import java.util.Arrays;
import java.util.Set;

public class FunctionCoreToolsHandlerImpl implements FunctionCoreToolsHandler {

    public static final String FUNC_EXTENSIONS_INSTALL_TEMPLATE = "func extensions install -c \"%s\"";
    public static final String INSTALL_FUNCTION_EXTENSIONS_FAIL = "Failed to install the Function extensions";
    public static final String NO_LOCAL_FUNCTION_CORE_TOOLS = "Local Azure Functions Core Tools does not " +
            "exist, please install Azure Function Core Tools and try again. See: https://aka.ms/azfunc-install";
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

    private static final Class[] FUNCTION_WITHOUT_FUNCTION_EXTENSION = {HttpBinding.class};
    private AbstractFunctionMojo mojo;
    private CommandHandler commandHandler;

    public FunctionCoreToolsHandlerImpl(final AbstractFunctionMojo mojo, final CommandHandler commandHandler) {
        this.mojo = mojo;
        this.commandHandler = commandHandler;
    }

    @Override
    public void installExtension(Set<Class> bindingTypes) throws Exception {
        final String localVersion = getLocalFunctionCoreToolsVersion();

        if (isLocalVersionSupportAutoInstall(localVersion)) {
            checkVersion(localVersion);
            installFunctionExtension();
        } else {
            if (checkExtensionNecessity(bindingTypes)) {
                this.mojo.error(NO_LOCAL_FUNCTION_CORE_TOOLS);
                throw new Exception(NO_LOCAL_FUNCTION_CORE_TOOLS);
            } else {
                this.mojo.warning(OUTDATED_LOCAL_FUNCTION_CORE_TOOLS);
            }
        }
    }

    protected boolean checkExtensionNecessity(Set<Class> bindingTypes) {
        return bindingTypes.stream().anyMatch(binding ->
                !Arrays.asList(FUNCTION_WITHOUT_FUNCTION_EXTENSION).contains(binding));
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

    protected boolean isLocalVersionSupportAutoInstall(final String localVersion) {
        if (localVersion == null || LEAST_SUPPORTED_VERSION.greaterThan(Version.valueOf(localVersion))) {
            return false;
        }
        return true;
    }

    protected void checkVersion(final String localVersion) throws Exception {
        final String latestCoreVersion = commandHandler.runCommandAndGetOutput(
                GET_LATEST_VERSION_CMD,
                false, /* showStdout */
                null /* workingDirectory */
        );

        try {
            if (localVersion == null || Version.valueOf(localVersion).lessThan(Version.valueOf(latestCoreVersion))) {
                this.mojo.warning(String.format(NEED_UPDATE_FUNCTION_CORE_TOOLS, localVersion, latestCoreVersion));
            }
        } catch (Exception e) {
            throw new Exception(GET_LATEST_VERSION_FAIL);
        }

    }

    protected String getLocalFunctionCoreToolsVersion() throws Exception {
        final String localVersion = commandHandler.runCommandAndGetOutput(
                GET_LOCAL_VERSION_CMD,
                false, /* showStdout */
                null /* workingDirectory */
        );

        try {
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
