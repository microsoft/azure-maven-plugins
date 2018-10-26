/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v2;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactHandlerImplV2Test {
    @Mock
    private AbstractWebAppMojo mojo;

    private ArtifactHandlerImplV2 handler;

    private ArtifactHandlerImplV2 handlerSpy;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new ArtifactHandlerImplV2(mojo);
        handlerSpy = spy(handler);
    }

    @Test
    public void publishViaWarDeploy() throws IOException, MojoExecutionException {
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(mojo).getDeployment();
        final List<Resource> resources = new ArrayList<Resource>();
        resources.add(new Resource());
        doReturn(resources).when(deployment).getResources();

        final String stagingDirectoryPath = "dummy";
        doReturn(stagingDirectoryPath).when(mojo).getDeploymentStagingDirectoryPath();
        doNothing().when(handlerSpy).copyArtifactsToStagingDirectory(resources, stagingDirectoryPath);

        final List<File> allArtifacts = new ArrayList<File>();
        allArtifacts.add(new File("dummypath\\dummy.war"));
        doReturn(allArtifacts).when(handlerSpy).getAllArtifacts(stagingDirectoryPath);

        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        doNothing().when(handlerSpy).publishArtifactsViaWarDeploy(target, stagingDirectoryPath, allArtifacts);
        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).publish(target);
        verify(handlerSpy, times(1)).
            copyArtifactsToStagingDirectory(resources, stagingDirectoryPath);
        verify(handlerSpy, times(1)).getAllArtifacts(stagingDirectoryPath);
        verify(handlerSpy, times(1))
            .publishArtifactsViaWarDeploy(target, stagingDirectoryPath, allArtifacts);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishViaZipDeploy() throws IOException, MojoExecutionException {
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(mojo).getDeployment();
        final List<Resource> resources = new ArrayList<Resource>();
        resources.add(new Resource());
        doReturn(resources).when(deployment).getResources();

        final String stagingDirectoryPath = "dummy";
        doReturn(stagingDirectoryPath).when(mojo).getDeploymentStagingDirectoryPath();
        doNothing().when(handlerSpy).copyArtifactsToStagingDirectory(resources, stagingDirectoryPath);

        final List<File> allArtifacts = new ArrayList<File>();
        allArtifacts.add(new File("dummypath\\dummy.jar"));
        doReturn(allArtifacts).when(handlerSpy).getAllArtifacts(stagingDirectoryPath);

        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        doNothing().when(handlerSpy).publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).publish(target);
        verify(handlerSpy, times(1)).
            copyArtifactsToStagingDirectory(resources, stagingDirectoryPath);
        verify(handlerSpy, times(1)).getAllArtifacts(stagingDirectoryPath);
        verify(handlerSpy, times(1)).publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void skipPublishWhenDeploymentNotSet() throws IOException, MojoExecutionException {
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(mojo).getDeployment();
        doReturn(Collections.emptyList()).when(deployment).getResources();
        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        final DeployTarget target = mock(DeployTarget.class);

        handlerSpy.publish(target);

        verify(mojo, times(1)).getLog();
        verify(handlerSpy, times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test(expected = MojoExecutionException.class)
    public void publishArtifactsViaWarDeployThrowException() throws MojoExecutionException {
        final DeployTarget target = mock(DeployTarget.class);
        final String stagingDirectoryPath = "";
        final List<File> warArtifacts = null;

        handlerSpy.publishArtifactsViaWarDeploy(target, stagingDirectoryPath, warArtifacts);
    }

    @Test
    public void publishArtifactsViaWarDeploy() throws MojoExecutionException {
        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new WebAppDeployTarget(app);
        final String stagingDirectory = Paths.get(Paths.get("").toAbsolutePath().toString(),
            "maven-plugin-temp").toString();
        final List<File> artifacts = new ArrayList<>();
        final File artifact = new File(Paths.get(stagingDirectory, "dummypath", "dummy.war").toString());
        artifacts.add(artifact);
        doNothing().when(handlerSpy).publishWarArtifact(target, artifact, "dummypath");

        handlerSpy.publishArtifactsViaWarDeploy(target, stagingDirectory, artifacts);
        verify(handlerSpy, times(1)).
            publishArtifactsViaWarDeploy(target, stagingDirectory, artifacts);
        verify(handlerSpy, times(1)).publishWarArtifact(target, artifact, "dummypath");
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishArtifactsViaZipDeploy() throws MojoExecutionException {
        final DeployTarget target = mock(DeployTarget.class);
        final File zipTestDirectory = new File("src/test/resources/artifacthandlerv2");
        final String stagingDirectoryPath = zipTestDirectory.getAbsolutePath();
        final File zipFile = new File(stagingDirectoryPath + ".zip");
        doNothing().when(target).zipDeploy(zipFile);

        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).info(anyString());

        handlerSpy.publishArtifactsViaZipDeploy(target, stagingDirectoryPath);

        verify(mojo, times(2)).getLog();
        verify(handlerSpy, times(1)).publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishWarArtifact() throws MojoExecutionException {
        final WebAppDeployTarget target = mock(WebAppDeployTarget.class);
        final File warArtifact = new File("D:\\temp\\dummypath");
        final String contextPath = "dummy";
        doNothing().when(target).warDeploy(warArtifact, contextPath);

        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).info(anyString());

        handlerSpy.publishWarArtifact(target, warArtifact, contextPath);

        verify(handlerSpy, times(1)).publishWarArtifact(target, warArtifact, contextPath);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test(expected = MojoExecutionException.class)
    public void publishWarArtifactThrowException() throws MojoExecutionException {
        final WebAppDeployTarget target = mock(WebAppDeployTarget.class);
        final File warArtifact = new File("D:\\temp\\dummypath");
        final String contextPath = "dummy";

        doThrow(RuntimeException.class).when(target).warDeploy(warArtifact, contextPath);

        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).info(anyString());
        handlerSpy.publishWarArtifact(target, warArtifact, contextPath);
    }
}
