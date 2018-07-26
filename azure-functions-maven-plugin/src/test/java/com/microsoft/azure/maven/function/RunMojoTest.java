/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.function.handlers.CommandHandler;
import com.microsoft.azure.maven.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.maven.function.utils.CommandUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.MockitoJUnitRunner;

import static com.microsoft.azure.maven.function.RunMojo.FUNC_CMD;
import static com.microsoft.azure.maven.function.RunMojo.FUNC_HOST_START_CMD;
import static com.microsoft.azure.maven.function.RunMojo.FUNC_HOST_START_WITH_DEBUG_CMD;
import static com.microsoft.azure.maven.function.RunMojo.RUNTIME_NOT_FOUND;
import static com.microsoft.azure.maven.function.RunMojo.RUN_FUNCTIONS_FAILURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RunMojoTest extends MojoTestBase {

    @Test
    public void doExecute() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        final RunMojo mojoSpy = spy(mojo);
        doNothing().when(mojoSpy).checkStageDirectoryExistence();
        doNothing().when(mojoSpy).checkRuntimeExistence(any(CommandHandler.class));
        doNothing().when(mojoSpy).runFunctions(any(CommandHandler.class));

        mojoSpy.doExecute();
        verify(mojoSpy, times(1)).checkStageDirectoryExistence();
        verify(mojoSpy, times(1)).checkRuntimeExistence(any(CommandHandler.class));
        verify(mojoSpy, times(1)).runFunctions(any(CommandHandler.class));
    }


    @Test(expected = MojoExecutionException.class)
    public void checkStageDirectoryExistenceWhenIsNotDirectory() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        final RunMojo mojoSpy = spy(mojo);

        doReturn("./RunMojoTest.java").when(mojoSpy).getDeploymentStageDirectory();
        mojoSpy.checkStageDirectoryExistence();
    }

    @Test(expected = MojoExecutionException.class)
    public void checkStageDirectoryExistenceWhenNotExisting() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        final RunMojo mojoSpy = spy(mojo);

        doReturn("./NotExistFile").when(mojoSpy).getDeploymentStageDirectory();
        mojoSpy.checkStageDirectoryExistence();
    }

    @Test
    public void checkRuntimeExistence() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        final RunMojo mojoSpy = spy(mojo);
        final CommandHandler commandHandlerMock = mock(CommandHandlerImpl.class);
        doNothing().when(commandHandlerMock).runCommandWithReturnCodeCheck(anyString(), anyBoolean(),
            anyString(), ArgumentMatchers.anyList(), anyString());
        mojoSpy.checkRuntimeExistence(commandHandlerMock);

        verify(commandHandlerMock, times(1))
            .runCommandWithReturnCodeCheck(
                mojoSpy.getCheckRuntimeCommand(),
                false,
                null,
                CommandUtils.getDefaultValidReturnCodes(),
                RUNTIME_NOT_FOUND
            );
    }

    @Test
    public void runFunctions() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        final RunMojo mojoSpy = spy(mojo);
        final CommandHandler commandHandlerMock = mock(CommandHandlerImpl.class);
        doNothing().when(commandHandlerMock).runCommandWithReturnCodeCheck(anyString(), anyBoolean(),
            anyString(), ArgumentMatchers.anyList(), anyString());
        doReturn("buildDirectory").when(mojoSpy).getDeploymentStageDirectory();
        mojoSpy.runFunctions(commandHandlerMock);

        verify(commandHandlerMock, times(1))
            .runCommandWithReturnCodeCheck(
                mojoSpy.getStartFunctionHostCommand(),
                true,
                mojoSpy.getDeploymentStageDirectory(),
                CommandUtils.getValidReturnCodes(),
                RUN_FUNCTIONS_FAILURE
            );
    }

    @Test
    public void getCheckRuntimeCommand() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        final RunMojo mojoSpy = spy(mojo);
        assertEquals(FUNC_CMD, mojoSpy.getCheckRuntimeCommand());
    }

    @Test
    public void getStartFunctionHostCommand() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        final RunMojo mojoSpy = spy(mojo);
        assertEquals(FUNC_HOST_START_CMD, mojoSpy.getStartFunctionHostCommand());

        System.setProperty("enableDebug", "true");
        assertEquals(FUNC_HOST_START_WITH_DEBUG_CMD, mojoSpy.getStartFunctionHostCommand());
    }

    private RunMojo getMojoFromPom() throws Exception {
        final RunMojo mojo = (RunMojo) getMojoFromPom("/pom.xml", "run");
        assertNotNull(mojo);
        return mojo;
    }
}
