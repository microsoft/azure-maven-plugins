/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RunMojoTest extends MojoTestBase {
    @Test
    public void doExecuteOnWindows() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        final RunMojo mojoSpy = spy(mojo);
        doNothing().when(mojoSpy).runCommand(any(String[].class), anyBoolean(), anyList(), anyString());
        doReturn("target").when(mojoSpy).getBuildDirectoryAbsolutePath();
        doReturn(true).when(mojoSpy).isWindows();

        mojoSpy.doExecute();
        final String stageDirectory = Paths.get("target", "azure-functions", "appName").toString();
        verify(mojoSpy, times(1)).runCommand(
                new String[]{"cmd.exe", "/c", "cd /D " + stageDirectory},
                false,
                Arrays.asList(0L),
                RunMojo.STAGE_DIR_NOT_FOUND);
        verify(mojoSpy, times(1)).runCommand(
                new String[]{"cmd.exe", "/c", "func"},
                false,
                Arrays.asList(0L),
                RunMojo.RUNTIME_NOT_FOUND);
        verify(mojoSpy, times(1)).runCommand(
                new String[]{"cmd.exe", "/c", "cd /D " + stageDirectory + " && func host start"},
                true,
                Arrays.asList(0L, 3221225786L),
                RunMojo.RUN_FUNCTIONS_FAILURE);
    }

    @Test
    public void doExecuteOnLinux() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        final RunMojo mojoSpy = spy(mojo);
        doNothing().when(mojoSpy).runCommand(any(String[].class), anyBoolean(), anyList(), anyString());
        doReturn("target").when(mojoSpy).getBuildDirectoryAbsolutePath();
        doReturn(false).when(mojoSpy).isWindows();

        mojoSpy.doExecute();
        final String stageDirectory = Paths.get("target", "azure-functions", "appName").toString();
        verify(mojoSpy, times(1)).runCommand(
                new String[]{"sh", "-c", "cd " + stageDirectory},
                false,
                Arrays.asList(0L),
                RunMojo.STAGE_DIR_NOT_FOUND);
        verify(mojoSpy, times(1)).runCommand(
                new String[]{"sh", "-c", "func"},
                false,
                Arrays.asList(0L),
                RunMojo.RUNTIME_NOT_FOUND);
        verify(mojoSpy, times(1)).runCommand(
                new String[]{"sh", "-c", "cd " + stageDirectory + "; func host start"},
                true,
                Arrays.asList(0L, 130L),
                RunMojo.RUN_FUNCTIONS_FAILURE);
    }

    @Test
    public void getRunFunctionCommandWithInputOnWindows() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        ReflectionUtils.setVariableValueInObject(mojo, "targetFunction", "httpTrigger");
        ReflectionUtils.setVariableValueInObject(mojo, "functionInputString", "input");
        final RunMojo mojoSpy = spy(mojo);
        doReturn("target").when(mojoSpy).getDeploymentStageDirectory();
        doReturn(true).when(mojoSpy).isWindows();

        final String[] command = mojoSpy.getRunFunctionCommand();

        assertEquals(3, command.length);
        assertEquals("cd /D target && func function run httpTrigger --no-interactive -c input", command[2]);
    }

    @Test
    public void getRunFunctionCommandWithInputFileOnWindows() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        ReflectionUtils.setVariableValueInObject(mojo, "targetFunction", "httpTrigger");
        ReflectionUtils.setVariableValueInObject(mojo, "functionInputFile", new File("pom.xml"));
        final RunMojo mojoSpy = spy(mojo);
        doReturn("target").when(mojoSpy).getDeploymentStageDirectory();
        doReturn(true).when(mojoSpy).isWindows();

        final String[] command = mojoSpy.getRunFunctionCommand();

        assertEquals(3, command.length);
        assertTrue(command[2].startsWith("cd /D target && func function run httpTrigger --no-interactive -f"));
    }

    @Test
    public void getRunFunctionCommandWithInputFileOnLinux() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        ReflectionUtils.setVariableValueInObject(mojo, "targetFunction", "httpTrigger");
        ReflectionUtils.setVariableValueInObject(mojo, "functionInputFile", new File("pom.xml"));
        final RunMojo mojoSpy = spy(mojo);
        doReturn("target").when(mojoSpy).getDeploymentStageDirectory();
        doReturn(false).when(mojoSpy).isWindows();

        final String[] command = mojoSpy.getRunFunctionCommand();

        assertEquals(3, command.length);
        assertTrue(command[2].startsWith("cd target; func function run httpTrigger --no-interactive -f"));
    }

    @Test
    public void getRunFunctionCommandWithInputOnLinux() throws Exception {
        final RunMojo mojo = getMojoFromPom();
        ReflectionUtils.setVariableValueInObject(mojo, "targetFunction", "httpTrigger");
        ReflectionUtils.setVariableValueInObject(mojo, "functionInputString", "input");
        final RunMojo mojoSpy = spy(mojo);
        doReturn("target").when(mojoSpy).getDeploymentStageDirectory();
        doReturn(false).when(mojoSpy).isWindows();

        final String[] command = mojoSpy.getRunFunctionCommand();

        assertEquals(3, command.length);
        assertEquals("cd target; func function run httpTrigger --no-interactive -c input", command[2]);
    }

    @Test
    public void runCommand() throws Exception {
        final RunMojo mojo = getMojoFromPom();

        final String[] command = mojo.isWindows() ?
                new String[]{"cmd.exe", "/c", "dir"} :
                new String[]{"sh", "-c", "ls -al"};
        mojo.runCommand(command, true, Arrays.asList(0L), "dir/ls error");
    }

    @Test
    public void handleExitValue() throws Exception {
        final RunMojo mojo = getMojoFromPom();

        final String errorMessage = "commandError";
        String caughtExceptionMessage = null;
        try {
            final InputStream inputStream = new ByteArrayInputStream("error details".getBytes());
            mojo.handleExitValue(1, Arrays.asList(0L), errorMessage, inputStream);
        } catch (Exception e) {
            caughtExceptionMessage = e.getMessage();
        } finally {
            assertEquals(errorMessage, caughtExceptionMessage);
        }
    }

    private RunMojo getMojoFromPom() throws Exception {
        final RunMojo mojo = (RunMojo) getMojoFromPom("/pom.xml", "run");
        assertNotNull(mojo);
        return mojo;
    }
}
