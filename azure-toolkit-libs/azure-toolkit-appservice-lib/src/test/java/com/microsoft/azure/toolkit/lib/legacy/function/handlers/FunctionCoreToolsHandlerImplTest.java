/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.handlers;

import com.microsoft.azure.toolkit.lib.legacy.function.utils.CommandUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.File;

import static com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandlerImpl.FUNC_EXTENSIONS_INSTALL_TEMPLATE;
import static com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandlerImpl.INSTALL_FUNCTION_EXTENSIONS_FAIL;
import static org.junit.Assert.assertEquals;

public class FunctionCoreToolsHandlerImplTest {

    @Test
    public void installExtension() throws Exception {
        final CommandHandler commandHandler = Mockito.mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = Mockito.spy(functionCoreToolsHandler);

        Mockito.doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        Mockito.doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        Mockito.doNothing().when(functionCoreToolsHandlerSpy).installFunctionExtension(new File("folder1"), new File("folder2"));

        functionCoreToolsHandlerSpy.installExtension(new File("folder1"), new File("folder2"));
    }

    @Test
    public void getLocalFunctionCoreToolsVersion() throws Exception {
        final CommandHandler commandHandler = Mockito.mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = Mockito.spy(functionCoreToolsHandler);
        Mockito.doReturn("2.0.1-beta.26")
                .when(commandHandler).runCommandAndGetOutput(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.any());
        assertEquals("2.0.1-beta.26", functionCoreToolsHandlerSpy.getLocalFunctionCoreToolsVersion());
    }

    @Test
    public void getLocalFunctionCoreToolsVersionFailed() throws Exception {
        final CommandHandler commandHandler = Mockito.mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = Mockito.spy(functionCoreToolsHandler);
        Mockito.doReturn("unexpected output")
                .when(commandHandler).runCommandAndGetOutput(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.any());
        Assert.assertNull(functionCoreToolsHandlerSpy.getLocalFunctionCoreToolsVersion());
    }

    @Test
    public void installFunctionExtension() throws Exception {
        final CommandHandler commandHandler = Mockito.mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = Mockito.spy(functionCoreToolsHandler);
        Mockito.doNothing().when(commandHandler).runCommandWithReturnCodeCheck(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyBoolean(), ArgumentMatchers.any(), ArgumentMatchers.anyList(), ArgumentMatchers.anyString());

        functionCoreToolsHandlerSpy.installFunctionExtension(new File("path1"), new File("path2"));
        Mockito.verify(commandHandler, Mockito.times(1)).runCommandWithReturnCodeCheck(
                String.format(FUNC_EXTENSIONS_INSTALL_TEMPLATE, new File("path2").getAbsolutePath()),
                true,
                new File("path1").getAbsolutePath(),
                CommandUtils.getDefaultValidReturnCodes(),
                INSTALL_FUNCTION_EXTENSIONS_FAIL
        );
    }

    @Test
    public void isLocalVersionSupportAutoInstall() {
        final CommandHandler commandHandler = Mockito.mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = Mockito.spy(functionCoreToolsHandler);

        Mockito.doReturn("2.0.1-beta.26").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        Mockito.doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
    }

    @Test(expected = Exception.class)
    public void isLocalVersionSupportAutoInstallWhenLocalVersionTooLow() throws Exception {
        final CommandHandler commandHandler = Mockito.mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = Mockito.spy(functionCoreToolsHandler);

        Mockito.doReturn("2.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        Mockito.doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        functionCoreToolsHandlerSpy.assureRequirementAddressed();
    }

    @Test
    public void checkVersion() throws Exception {
        final CommandHandler commandHandler = Mockito.mock(CommandHandler.class);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandler =
                new FunctionCoreToolsHandlerImpl(commandHandler);
        final FunctionCoreToolsHandlerImpl functionCoreToolsHandlerSpy = Mockito.spy(functionCoreToolsHandler);

        // Equal to newest version
        Mockito.doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        Mockito.doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        functionCoreToolsHandlerSpy.assureRequirementAddressed();

        // Less than newest version
        Mockito.doReturn("2.0.1-beta.27").when(functionCoreToolsHandlerSpy).getLocalFunctionCoreToolsVersion();
        Mockito.doReturn("3.0.0").when(functionCoreToolsHandlerSpy).getLatestFunctionCoreToolsVersion();
        functionCoreToolsHandlerSpy.assureRequirementAddressed();
    }
}
