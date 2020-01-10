/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.artifact;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.ProjectUtils;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.appservice.OperatingSystemEnum;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import com.microsoft.azure.maven.webapp.utils.TestUtils;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.model.Resource;
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
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
            .build();
        handlerSpy = spy(handler);
    }

    @Test
    public void publishViaWarDeploy() throws IOException, AzureExecutionException {

    }

    @Test
    public void publishViaZipDeploy() throws IOException, AzureExecutionException {

        final String stagingDirectoryPath = "dummy";
        doReturn(stagingDirectoryPath).when(mojo).getDeploymentStagingDirectoryPath();

        buildHandler();

        final List<File> allArtifacts = new ArrayList<File>();
        allArtifacts.add(new File("dummypath\\dummy.jar"));
        doReturn(allArtifacts).when(handlerSpy).getAllArtifacts(stagingDirectoryPath);

        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        doNothing().when(handlerSpy).publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).publish(target);
        verify(handlerSpy, times(1)).getAllArtifacts(stagingDirectoryPath);
        verify(handlerSpy, times(1)).publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test(expected = AzureExecutionException.class)
    public void publishArtifactsViaWarDeployThrowException() throws AzureExecutionException {
        final DeployTarget target = mock(DeployTarget.class);
        final String stagingDirectoryPath = "";
        final List<File> warArtifacts = null;
        buildHandler();
        handlerSpy.publishArtifactsViaWarDeploy(target, stagingDirectoryPath, warArtifacts);
    }

    @Test
    public void publishArtifactsViaWarDeploy() throws AzureExecutionException {
        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new WebAppDeployTarget(app);
        final String stagingDirectory = Paths.get(Paths.get("").toAbsolutePath().toString(),
            "maven-plugin-temp").toString();
        final List<File> artifacts = new ArrayList<>();
        final File artifact = new File(Paths.get(stagingDirectory, "dummypath", "dummy.war").toString());
        artifacts.add(artifact);
        buildHandler();
        doNothing().when(handlerSpy).publishWarArtifact(target, artifact, "dummypath");
        handlerSpy.publishArtifactsViaWarDeploy(target, stagingDirectory, artifacts);
        verify(handlerSpy, times(1)).
            publishArtifactsViaWarDeploy(target, stagingDirectory, artifacts);
        verify(handlerSpy, times(1)).publishWarArtifact(target, artifact, "dummypath");
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishArtifactsViaZipDeploy() throws AzureExecutionException {
        final DeployTarget target = mock(DeployTarget.class);
        final File zipTestDirectory = new File("src/test/resources/artifacthandlerv2");
        final String stagingDirectoryPath = zipTestDirectory.getAbsolutePath();
        doNothing().when(target).zipDeploy(any());

        buildHandler();
        doReturn(false).when(handlerSpy).isJavaSERuntime();
        handlerSpy.publishArtifactsViaZipDeploy(target, stagingDirectoryPath);

        verify(handlerSpy, times(1)).isJavaSERuntime();
        verify(handlerSpy, times(1)).publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void isJavaSERuntime() throws Exception {
        final MavenProject mavenProject = TestUtils.getSimpleMavenProjectForUnitTest();
        final IProject project = ProjectUtils.convertCommonProject(mavenProject);
        final RuntimeSetting runtimeSetting = mock(RuntimeSetting.class);
        handler = new ArtifactHandlerImplV2.Builder()
                .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
                .project(project)
                .runtime(runtimeSetting)
                .build();
        handlerSpy = spy(handler);

        doReturn(false).when(runtimeSetting).isEmpty();
        doReturn(OperatingSystemEnum.Windows).when(runtimeSetting).getOsEnum();
        doReturn(WebContainer.fromString("java 8")).when(runtimeSetting).getWebContainer();
        assertTrue(handlerSpy.isJavaSERuntime());

        // No runtime setting, just check project packaging
        FieldUtils.writeField(project, "artifactFile", Paths.get("artifactFile.war"), true);
        doReturn(true).when(runtimeSetting).isEmpty();
        assertFalse(handlerSpy.isJavaSERuntime());

        FieldUtils.writeField(project, "artifactFile", Paths.get("artifactFile.jar"), true);
        doReturn(true).when(runtimeSetting).isEmpty();
        assertTrue(handlerSpy.isJavaSERuntime());

        // Project with jar packaging will always be regarded as java se project
        Mockito.reset(runtimeSetting);
        doReturn(false).when(runtimeSetting).isEmpty(   );
        FieldUtils.writeField(project, "artifactFile", Paths.get("artifactFile.jar"), true);
        assertTrue(handlerSpy.isJavaSERuntime());
        verify(runtimeSetting, times(0)).getOsEnum();
        verify(runtimeSetting, times(0)).getWebContainer();
        verify(runtimeSetting, times(0)).getLinuxRuntime();
    }

    @Test
    public void publishWarArtifact() throws AzureExecutionException {
        final WebAppDeployTarget target = mock(WebAppDeployTarget.class);
        final File warArtifact = new File("D:\\temp\\dummypath");
        final String contextPath = "dummy";
        doNothing().when(target).warDeploy(warArtifact, contextPath);

        buildHandler();
        handlerSpy.publishWarArtifact(target, warArtifact, contextPath);

        verify(handlerSpy, times(1)).publishWarArtifact(target, warArtifact, contextPath);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test(expected = AzureExecutionException.class)
    public void publishWarArtifactThrowException() throws AzureExecutionException {
        final WebAppDeployTarget target = mock(WebAppDeployTarget.class);
        final File warArtifact = new File("D:\\temp\\dummypath");
        final String contextPath = "dummy";

        doThrow(RuntimeException.class).when(target).warDeploy(warArtifact, contextPath);

        buildHandler();
        handlerSpy.publishWarArtifact(target, warArtifact, contextPath);
    }
}
