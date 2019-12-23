/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.maven.function.AbstractFunctionMojo;
import com.microsoft.azure.maven.function.utils.CommandUtils;

import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static com.microsoft.azure.maven.function.handlers.FunctionCoreToolsHandlerImpl.FUNC_EXTENSIONS_INSTALL_TEMPLATE;
import static com.microsoft.azure.maven.function.handlers.FunctionCoreToolsHandlerImpl.INSTALL_FUNCTION_EXTENSIONS_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FunctionCoreToolsHandlerImplTest {

    @Test
    public void installExtension() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);

        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        doNothing().when(functionCoreToolsHandlerSpy).installFunctionExtension();

        functionCoreToolsHandlerSpy.installExtension();
    }

    @Test
    public void getLocalFunctionCoreToolsVersion() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);
        doReturn("2.0.1-beta.26")
                .when(commandHandler).runCommandAndGetOutput(anyString(), anyBoolean(), any());
        assertEquals("2.0.1-beta.26", functionCoreToolsHandlerSpy.getLocalFunctionCoreToolsVersion());
    }

    @Test
    public void getLocalFunctionCoreToolsVersionFailed() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);
        doReturn("unexpected output")
                .when(commandHandler).runCommandAndGetOutput(anyString(), anyBoolean(), any());
        assertNull(functionCoreToolsHandlerSpy.getLocalFunctionCoreToolsVersion());
    }

    @Test
    public void installFunctionExtension() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);
        doNothing().when(commandHandler).runCommandWithReturnCodeCheck(anyString(),
                anyBoolean(), any(), ArgumentMatchers.anyList(), anyString());
        doReturn("path").when(functionCoreToolsHandlerSpy).getProjectBasePath();
        doReturn("path").when(mojo).getDeploymentStagingDirectoryPath();

        functionCoreToolsHandlerSpy.installFunctionExtension();
        verify(commandHandler, times(1)).runCommandWithReturnCodeCheck(
                String.format(FUNC_EXTENSIONS_INSTALL_TEMPLATE, functionCoreToolsHandlerSpy.getProjectBasePath()),
                true,
                mojo.getDeploymentStagingDirectoryPath(),
                CommandUtils.getDefaultValidReturnCodes(),
                INSTALL_FUNCTION_EXTENSIONS_FAIL
        );
    }

    @Test
    public void isLocalVersionSupportAutoInstall() {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);

        doReturn("2.0.1-beta.26").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
    }

    @Test(expected = Exception.class)
    public void isLocalVersionSupportAutoInstallWhenLocalVersionTooLow() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);

        doReturn("2.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        functionCoreToolsHandlerSpy.assureRequirementAddressed();
    }

    @Test
    public void checkVersion() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);

        // Equal to newest version
        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        functionCoreToolsHandlerSpy.assureRequirementAddressed();

        // Less than newest version
        reset(mojo);
        doReturn("2.0.1-beta.27").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        functionCoreToolsHandlerSpy.assureRequirementAddressed();
    }
}
