/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.maven.function.AbstractFunctionMojo;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class FunctionCoreToolsHandlerImplTest {

    @Test
    public void installExtension() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo.getLog(), commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);

        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        doNothing().when(functionCoreToolsHandlerSpy).installFunctionExtension(new File("path1"), new File("path2"));

        functionCoreToolsHandlerSpy.installExtension(new File("path1"), new File("path2"));
        verify(mojo, never()).warning(anyString());
    }

    @Test
    public void getLocalFunctionCoreToolsVersion() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo.getLog(), commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);
        doReturn("2.0.1-beta.26")
                .when(commandHandler).runCommandAndGetOutput(anyString(), anyBoolean(), any());
        assertEquals("2.0.1-beta.26", functionCoreToolsHandlerSpy.getLocalFunctionCoreToolsVersion());
    }

    @Test
    public void getLocalFunctionCoreToolsVersionFailed() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).warn(anyString());
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo.getLog(), commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);
        doReturn("unexpected output")
                .when(commandHandler).runCommandAndGetOutput(anyString(), anyBoolean(), any());
        assertNull(functionCoreToolsHandlerSpy.getLocalFunctionCoreToolsVersion());
    }

    @Test
    public void isLocalVersionSupportAutoInstall() {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo.getLog(), commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);

        doReturn("2.0.1-beta.26").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        verify(mojo, never()).warning(anyString());
    }

    @Test(expected = Exception.class)
    public void isLocalVersionSupportAutoInstallWhenLocalVersionTooLow() throws Exception {
        final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        final CommandHandler commandHandler = mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(mojo.getLog(), commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);

        doReturn("2.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        functionCoreToolsHandlerSpy.assureRequirementAddressed();
    }

    @Test
    public void checkVersion() throws Exception {
        //    final AbstractFunctionMojo mojo = mock(AbstractFunctionMojo.class);
        //    final CommandHandler commandHandler = mock(CommandHandler.class);
        //    final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
        //            new FunctionCoreToolsHandlerImpl(mojo.getLog(), commandHandler);
        //    final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = spy(functionCoreToolsHandler);
        //
        //    // Equal to newest version
        //    doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        //    doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        //    functionCoreToolsHandlerSpy.assureRequirementAddressed();
        //    verify(mojo, never()).warning(anyString());
        //
        //    // Less than newest version
        //    reset(mojo);
        //    doReturn("2.0.1-beta.27").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        //    doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        //    functionCoreToolsHandlerSpy.assureRequirementAddressed();
        //    verify(mojo, times(1)).warning(anyString());
    }
}
