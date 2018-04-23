/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.file.Paths;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;

public class WarArtifactHandlerImplTest {

    @Mock
    private AbstractWebAppMojo mojo;

    private WarArtifactHandlerImpl handler = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new WarArtifactHandlerImpl(mojo);
    }

    @Test
    public void publish() throws Exception {
        final WarArtifactHandlerImpl handlerSpy = spy(handler);
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

        handlerSpy.publish();
        verify(appMock, times(1)).warDeploy(file, path);
    }

    @Test
    public void getContextPath() {
        final WarArtifactHandlerImpl handlerSpy = spy(handler);
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
        final WarArtifactHandlerImpl handlerSpy = spy(handler);
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
        final WarArtifactHandlerImpl handlerSpy = spy(handler);
        handlerSpy.assureWarFileExisted(new File("test.jar"));
    }

    @Test(expected = MojoExecutionException.class)
    public void assureWarFileExistedWhenFileNotExist() throws MojoExecutionException {
        final WarArtifactHandlerImpl handlerSpy = spy(handler);
        final File fileMock = mock(File.class);

        doReturn("test.war").when(fileMock).getName();
        doReturn(false).when(fileMock).exists();
        handlerSpy.assureWarFileExisted(fileMock);
    }

    @Test(expected = MojoExecutionException.class)
    public void assureWarFileExistedWhenIsNotAFile() throws MojoExecutionException {
        final WarArtifactHandlerImpl handlerSpy = spy(handler);
        final File fileMock = mock(File.class);

        doReturn("test.war").when(fileMock).getName();
        doReturn(false).when(fileMock).exists();
        doReturn(false).when(fileMock).isFile();
        handlerSpy.assureWarFileExisted(fileMock);
    }
}
