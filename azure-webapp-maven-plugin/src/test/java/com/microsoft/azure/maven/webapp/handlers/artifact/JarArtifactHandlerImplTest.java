/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.artifact;

import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.maven.ProjectUtils;
import com.microsoft.azure.maven.common.utils.SneakyThrowUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.deploytarget.DeploymentSlotDeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import com.microsoft.azure.maven.webapp.utils.TestUtils;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JarArtifactHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private JarArtifactHandlerImpl handler;
    private JarArtifactHandlerImpl handlerSpy;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void buildHandler() {
        try {
            when(mojo.getProject()).thenReturn(TestUtils.getSimpleMavenProjectForUnitTest());
            handler = (JarArtifactHandlerImpl) new JarArtifactHandlerImpl.Builder().jarFile(mojo.getJarFile())
                    .project(ProjectUtils.convertCommonProject(mojo.getProject()))
                    .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath()).build();
            handlerSpy = spy(handler);
        } catch (Exception e) {
            SneakyThrowUtils.sneakyThrow(e);
        }

    }

    @Test
    public void publish() throws Exception {
        final DeployTarget deployTarget = new WebAppDeployTarget(this.mojo.getWebApp());
        buildHandler();
        doNothing().when(handlerSpy).publish(deployTarget);
        handlerSpy.publish(deployTarget);
        verify(handlerSpy).publish(deployTarget);
    }

    @Test
    public void publishToDeploymentSlot() throws Exception {
        final DeploymentSlot slot = mock(DeploymentSlot.class);
        final DeployTarget deployTarget = new DeploymentSlotDeployTarget(slot);
        buildHandler();
        doNothing().when(handlerSpy).publish(deployTarget);

        handlerSpy.publish(deployTarget);
        verify(handlerSpy).publish(deployTarget);
    }

    @Test
    public void prepareDeploymentFiles() throws IOException {

    }

    @Test
    public void generateWebConfigFile() {

    }

    @Test
    public void getJarFile() {
        doReturn("test.jar").when(mojo).getJarFile();
        buildHandler();
        assertEquals("test.jar", handlerSpy.getJarFile().getName());

        final MavenProject project = mock(MavenProject.class);
        doReturn(project).when(mojo).getProject();
        buildHandler();
        assertEquals("test.jar", handlerSpy.getJarFile().getName());
        assertEquals("test.jar", handlerSpy.getJarFile().getName());
    }

    @Test(expected = AzureExecutionException.class)
    public void assureJarFileExistedWhenFileExtWrong() throws AzureExecutionException {
        buildHandler();
        handlerSpy.assureJarFileExisted(new File("test.jar"));
    }

    @Test(expected = AzureExecutionException.class)
    public void assureJarFileExistedWhenFileNotExist() throws AzureExecutionException {
        buildHandler();
        final File fileMock = mock(File.class);

        doReturn("test.jar").when(fileMock).getName();
        doReturn(false).when(fileMock).exists();
        handlerSpy.assureJarFileExisted(fileMock);
    }

    @Test(expected = AzureExecutionException.class)
    public void assureJarFileExistedWhenIsNotAFile() throws AzureExecutionException {
        final File fileMock = mock(File.class);

        doReturn("test.jar").when(fileMock).getName();
        doReturn(false).when(fileMock).exists();
        buildHandler();
        handlerSpy.assureJarFileExisted(fileMock);
    }

    @Test
    public void assureJarFileExisted() throws AzureExecutionException {
        final File file = mock(File.class);
        doReturn("test.jar").when(file).getName();
        doReturn(true).when(file).exists();
        doReturn(true).when(file).isFile();
        buildHandler();
        handlerSpy.assureJarFileExisted(file);

        verify(file).getName();
        verify(file).exists();
        verify(file).isFile();
    }
}
