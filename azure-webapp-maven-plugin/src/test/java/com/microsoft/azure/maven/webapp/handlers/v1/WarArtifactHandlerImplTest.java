/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v1;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.deploytarget.DeploymentSlotDeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WarArtifactHandlerImplTest {

    @Mock
    private AbstractWebAppMojo mojo;

    private WarArtifactHandlerImpl handler = null;

    private WarArtifactHandlerImpl handlerSpy = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new WarArtifactHandlerImpl(mojo);
        handlerSpy = spy(handler);
    }

    @Test
    public void publish() throws Exception {
        final File file = new File("");
        final String path = "";
        doReturn(file).when(handlerSpy).getWarFile();
        doNothing().when(handlerSpy).assureWarFileExisted(any(File.class));
        doReturn(path).when(handlerSpy).getContextPath();

        final Log logMock = mock(Log.class);
        final WebApp appMock = mock(WebApp.class);
        doReturn(logMock).when(mojo).getLog();
        doNothing().when(logMock).info(anyString());
        doReturn(appMock).when(mojo).getWebApp();
        doNothing().when(appMock).warDeploy(any(File.class), anyString());

        handlerSpy.publish(new WebAppDeployTarget(appMock));
        verify(appMock, times(1)).warDeploy(file, path);
    }

    @Test
    public void publishToDeploymentSlot() throws Exception {
        final File file = new File("");
        final String path = "";
        doReturn(file).when(handlerSpy).getWarFile();
        doNothing().when(handlerSpy).assureWarFileExisted(any(File.class));
        doReturn(path).when(handlerSpy).getContextPath();

        final Log logMock = mock(Log.class);
        final WebApp appMock = mock(WebApp.class);
        final DeploymentSlotSetting slotSettingMock = mock(DeploymentSlotSetting.class);
        final DeploymentSlot slotMock = mock(DeploymentSlot.class);
        doReturn(logMock).when(mojo).getLog();
        doReturn(appMock).when(mojo).getWebApp();
        doReturn(slotSettingMock).when(mojo).getDeploymentSlotSetting();
        doNothing().when(logMock).info(anyString());
        doReturn("").when(slotSettingMock).getName();
        doReturn(slotMock).when(mojo).getDeploymentSlot(appMock, "");
        doNothing().when(slotMock).warDeploy(any(File.class), anyString());
        handlerSpy.publish(new DeploymentSlotDeployTarget(slotMock));

        verify(slotMock, times(1)).warDeploy(file, path);
    }

    @Test(expected = MojoExecutionException.class)
    public void publishFailed() throws Exception {
        final File file = new File("");
        doReturn(file).when(handlerSpy).getWarFile();
        doNothing().when(handlerSpy).assureWarFileExisted(any(File.class));
        doReturn("").when(handlerSpy).getContextPath();

        final Log log = mock(Log.class);
        final WebApp app = mock(WebApp.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).info(anyString());
        doReturn(app).when(mojo).getWebApp();
        doThrow(RuntimeException.class).when(app).warDeploy(file, "");

        handlerSpy.publish(new WebAppDeployTarget(app));
    }

    @Test
    public void publishThrowException() throws MojoExecutionException {
        final File file = new File("");
        final String path = "";
        doReturn(file).when(handlerSpy).getWarFile();
        doNothing().when(handlerSpy).assureWarFileExisted(any(File.class));
        doReturn(path).when(handlerSpy).getContextPath();
        final WebApp app = mock(WebApp.class);
        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();

        try {
            handlerSpy.publish(new WebAppDeployTarget(app));
        } catch (final MojoExecutionException e) {
            assertEquals("Failed to deploy war file after 3 times of retry.", e.getMessage());
        }
    }

    @Test
    public void getContextPath() {
        doReturn("/").when(mojo).getPath();
        assertEquals(handlerSpy.getContextPath(), "");

        doReturn("  /  ").when(mojo).getPath();
        assertEquals(handlerSpy.getContextPath(), "");

        doReturn("/test").when(mojo).getPath();
        assertEquals(handlerSpy.getContextPath(), "test");

        doReturn("test").when(mojo).getPath();
        assertEquals(handlerSpy.getContextPath(), "test");

        doReturn("/test/test").when(mojo).getPath();
        assertEquals(handlerSpy.getContextPath(), "test/test");

        doReturn("test/test").when(mojo).getPath();
        assertEquals(handlerSpy.getContextPath(), "test/test");
    }

    @Test
    public void getWarFile() {
        doReturn(null).when(mojo).getWarFile();
        doReturn("buildDirectory").when(mojo).getBuildDirectoryAbsolutePath();

        final MavenProject projectMock = mock(MavenProject.class);
        final Build buildMock = mock(Build.class);
        doReturn(projectMock).when(mojo).getProject();
        doReturn(buildMock).when(projectMock).getBuild();
        doReturn("finalName").when(buildMock).getFinalName();

        final File customWarFile = handlerSpy.getWarFile();
        assertNotNull(customWarFile);
        assertEquals(customWarFile.getPath(), Paths.get("buildDirectory/finalName.war").toString());

        doReturn("warFile.war").when(mojo).getWarFile();
        final File defaultWar = handlerSpy.getWarFile();
        assertNotNull(defaultWar);
        assertEquals(defaultWar.getPath(), Paths.get("warFile.war").toString());
    }

    @Test(expected = MojoExecutionException.class)
    public void assureWarFileExistedWhenFileExtWrong() throws MojoExecutionException {
        handlerSpy.assureWarFileExisted(new File("test.jar"));
    }

    @Test(expected = MojoExecutionException.class)
    public void assureWarFileExistedWhenFileNotExist() throws MojoExecutionException {
        final File fileMock = mock(File.class);

        doReturn("test.war").when(fileMock).getName();
        doReturn(false).when(fileMock).exists();
        handlerSpy.assureWarFileExisted(fileMock);
    }

    @Test(expected = MojoExecutionException.class)
    public void assureWarFileExistedWhenIsNotAFile() throws MojoExecutionException {
        final File fileMock = mock(File.class);

        doReturn("test.war").when(fileMock).getName();
        doReturn(false).when(fileMock).exists();
        doReturn(false).when(fileMock).isFile();
        handlerSpy.assureWarFileExisted(fileMock);
    }

    @Test
    public void assureWarFileExisted() throws MojoExecutionException {
        final File file = mock(File.class);
        doReturn("test.war").when(file).getName();
        doReturn(true).when(file).exists();
        doReturn(true).when(file).isFile();

        handlerSpy.assureWarFileExisted(file);

        verify(file).getName();
        verify(file).exists();
        verify(file).isFile();
    }
}
