/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.zeroturnaround.zip.ZipException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ZIPArtifactHandlerImplTest {
    @Mock
    private AbstractAppServiceMojo mojo;

    private ZIPArtifactHandlerImpl handler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new ZIPArtifactHandlerImpl(mojo);
    }

    @Test
    public void publish() throws MojoExecutionException, IOException {
        final ZIPArtifactHandlerImpl handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        final File file = mock(File.class);

        doReturn(file).when(handlerSpy).getZipFile();
        doNothing().when(app).zipDeploy(file);
        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();

        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).prepareResources();
        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
        verify(handlerSpy, times(1)).getZipFile();
        verify(handlerSpy, times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void assureStagingDirectoryNotEmpty() throws MojoExecutionException {
        final ZIPArtifactHandlerImpl handlerSpy = spy(handler);
        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        handlerSpy.assureStagingDirectoryNotEmpty();
        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
    }

    @Test(expected = MojoExecutionException.class)
    public void assureStagingDirectoryNotEmptyThrowException() throws MojoExecutionException {
        final ZIPArtifactHandlerImpl handlerSpy = spy(handler);
        doReturn("").when(handlerSpy).getDeploymentStagingDirectoryPath();
        handlerSpy.assureStagingDirectoryNotEmpty();
    }

    @Test
    public void getZipFile() {
        final ZIPArtifactHandlerImpl handlerSpy = spy(handler);
        final File zipTestDirectory = new File("src/test/resources/ziptest");
        doReturn(zipTestDirectory.getAbsolutePath()).when(handlerSpy).getDeploymentStagingDirectoryPath();

        assertEquals(zipTestDirectory.getAbsolutePath() + ".zip", handlerSpy.getZipFile().getAbsolutePath());
    }

    @Test(expected = ZipException.class)
    public void getZipFileThrowException() {
        final ZIPArtifactHandlerImpl handlerSpy = spy(handler);
        doReturn("").when(handlerSpy).getDeploymentStagingDirectoryPath();

        handlerSpy.getZipFile();
    }

    @Test
    public void getWebAppDeploymentStagingDirectoryPath() {
        final ZIPArtifactHandlerImpl handlerSpy = spy(handler);
        final String mockPluginName = "azure-webapp-maven-plugin";
        final String mockBuildDirectoryPath = "D:\\test-project\\target";
        final String mockAppName = "test-webapp";
        doReturn(mockPluginName).when(mojo).getPluginName();
        doReturn(mockBuildDirectoryPath).when(mojo).getBuildDirectoryAbsolutePath();
        doReturn(mockAppName).when(mojo).getAppName();
        final String result = handlerSpy.getDeploymentStagingDirectoryPath();
        assertEquals(result, Paths.get(mockBuildDirectoryPath, "azure-webapp", mockAppName).toString());
    }

    @Test
    public void getFunctionDeploymentStagingDirectoryPath() {
        final ZIPArtifactHandlerImpl handlerSpy = spy(handler);
        final String mockPluginName = "azure-functions-maven-plugin";
        final String mockBuildDirectoryPath = "D:\\test-project\\target";
        final String mockAppName = "test-functions";
        doReturn(mockPluginName).when(mojo).getPluginName();
        doReturn(mockBuildDirectoryPath).when(mojo).getBuildDirectoryAbsolutePath();
        doReturn(mockAppName).when(mojo).getAppName();
        final String result = handlerSpy.getDeploymentStagingDirectoryPath();
        assertEquals(result, Paths.get(mockBuildDirectoryPath, "azure-functions", mockAppName).toString());
    }
}

