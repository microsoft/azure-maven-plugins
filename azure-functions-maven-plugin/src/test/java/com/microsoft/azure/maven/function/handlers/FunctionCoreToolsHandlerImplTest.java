/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import static com.microsoft.azure.maven.function.handlers.FunctionCoreToolsHandlerImpl.FUNC_EXTENSIONS_INSTALL_TEMPLATE;
import static com.microsoft.azure.maven.function.handlers.FunctionCoreToolsHandlerImpl.INSTALL_FUNCTION_EXTENSIONS_FAIL;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import com.github.zafarkhaja.semver.Version;
import com.microsoft.azure.maven.function.AbstractFunctionMojo;
import com.microsoft.azure.maven.function.utils.CommandUtils;

public class FunctionCoreToolsHandlerImplTest {

    @Test
    public void installExtension() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);

        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        doReturn(true).when(functionCoreToolsHandlerSpy).isLocalVersionSupportAutoInstall(anyString());
        doNothing().when(functionCoreToolsHandlerSpy).installFunctionExtension();
        doNothing().when(functionCoreToolsHandlerSpy).checkVersion(any());

        functionCoreToolsHandlerSpy.installExtension();
        verify(mojo, never()).warning(anyString());
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
        doReturn("path").when(mojo).getDeploymentStageDirectory();

        functionCoreToolsHandlerSpy.installFunctionExtension();
        verify(commandHandler, times(1)).runCommandWithReturnCodeCheck(
                String.format(FUNC_EXTENSIONS_INSTALL_TEMPLATE, functionCoreToolsHandlerSpy.getProjectBasePath()),
                true,
                mojo.getDeploymentStageDirectory(),
                CommandUtils.getDefaultValidReturnCodes(),
                INSTALL_FUNCTION_EXTENSIONS_FAIL
        );
    }

    @Test
    public void isLocalVersionSupportAutoInstall() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        assertTrue(functionCoreToolsHandler.isLocalVersionSupportAutoInstall("2.0.1-beta.26"));
    }

    @Test
    public void assureLocalVersionSupportAutoInstallWhenPassingNull() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        assertFalse(functionCoreToolsHandler.isLocalVersionSupportAutoInstall(null));
    }

    @Test
    public void isLocalVersionSupportAutoInstallWhenLocalVersionTooLow() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        assertFalse(functionCoreToolsHandler.isLocalVersionSupportAutoInstall("2.0.0"));
    }

    @Test
    public void checkVersion() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo, commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);

        // Equal to newest version
        doReturn("3.0.0").when(commandHandler).runCommandAndGetOutput(anyString(),
                anyBoolean(), any());
        functionCoreToolsHandlerSpy.checkVersion("3.0.0");
        verify(mojo, never()).warning(anyString());

        // Less than least supported version
        reset(mojo);
        doReturn("2.0.1-beta.26").when(commandHandler).runCommandAndGetOutput(anyString(),
                anyBoolean(), any());
        functionCoreToolsHandlerSpy.checkVersion("2.0.1-beta.24");
        verify(mojo, times(1)).warning(anyString());

        // Less than newest version but higher than least supported version
        reset(mojo);
        doReturn("2.0.1-beta.27").when(commandHandler).runCommandAndGetOutput(anyString(),
                anyBoolean(), any());
        functionCoreToolsHandlerSpy.checkVersion("2.0.1-beta.26");
        verify(mojo, times(1)).warning(anyString());
    }
}
