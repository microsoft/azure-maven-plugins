/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.artifact;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.appservice.OperatingSystemEnum;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    }

    public void buildHandler() {
        handler = new ArtifactHandlerImplV2.Builder()
            .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
            .log(mojo.getLog())
            .resources(mojo.getDeployment().getResources())
            .build();
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
        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).info(anyString());

        buildHandler();
        doNothing().when(handlerSpy).copyArtifactsToStagingDirectory();

        final List<File> allArtifacts = new ArrayList<File>();
        allArtifacts.add(new File("dummypath\\dummy.war"));
        doReturn(allArtifacts).when(handlerSpy).getAllArtifacts(stagingDirectoryPath);

        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        doNothing().when(handlerSpy).publishArtifactsViaWarDeploy(target, stagingDirectoryPath, allArtifacts);

        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).publish(target);
        verify(handlerSpy, times(1)).copyArtifactsToStagingDirectory();
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
        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).info(anyString());

        buildHandler();
        doNothing().when(handlerSpy).copyArtifactsToStagingDirectory();

        final List<File> allArtifacts = new ArrayList<File>();
        allArtifacts.add(new File("dummypath\\dummy.jar"));
        doReturn(allArtifacts).when(handlerSpy).getAllArtifacts(stagingDirectoryPath);

        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        doNothing().when(handlerSpy).publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).publish(target);
        verify(handlerSpy, times(1)).copyArtifactsToStagingDirectory();
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
        buildHandler();
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
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(mojo).getDeployment();
        buildHandler();
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
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(mojo).getDeployment();
        buildHandler();
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
        doNothing().when(target).zipDeploy(any());

        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).info(anyString());
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(mojo).getDeployment();
        buildHandler();
        doReturn(false).when(handlerSpy).isJavaSERuntime();
        handlerSpy.publishArtifactsViaZipDeploy(target, stagingDirectoryPath);

        verify(mojo, times(1)).getLog();
        verify(handlerSpy,times(1)).isJavaSERuntime();
        verify(handlerSpy, times(1)).publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void isJavaSERuntime() {
        final MavenProject mavenProject = mock(MavenProject.class);
        final RuntimeSetting runtimeSetting = mock(RuntimeSetting.class);
        handler = new ArtifactHandlerImplV2.Builder()
                .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
                .log(mojo.getLog())
                .project(mavenProject)
                .runtime(runtimeSetting)
                .build();
        handlerSpy = spy(handler);

        doReturn(false).when(runtimeSetting).isEmpty();
        doReturn(OperatingSystemEnum.Windows).when(runtimeSetting).getOsEnum();
        doReturn(WebContainer.fromString("java 8")).when(runtimeSetting).getWebContainer();
        assertTrue(handlerSpy.isJavaSERuntime());

        // No runtime setting, just check project packaging
        doReturn("war").when(mavenProject).getPackaging();
        doReturn(true).when(runtimeSetting).isEmpty();
        assertFalse(handlerSpy.isJavaSERuntime());

        doReturn("jar").when(mavenProject).getPackaging();
        doReturn(true).when(runtimeSetting).isEmpty();
        assertTrue(handlerSpy.isJavaSERuntime());


        // Project with jar packaging will always be regarded as java se project
        Mockito.reset(runtimeSetting);
        doReturn(false).when(runtimeSetting).isEmpty();
        doReturn("jar").when(mavenProject).getPackaging();
        assertTrue(handlerSpy.isJavaSERuntime());
        verify(runtimeSetting, times(0)).getOsEnum();
        verify(runtimeSetting, times(0)).getWebContainer();
        verify(runtimeSetting, times(0)).getLinuxRuntime();
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
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(mojo).getDeployment();

        buildHandler();
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
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(mojo).getDeployment();
        buildHandler();
        handlerSpy.publishWarArtifact(target, warArtifact, contextPath);
    }
}
