/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;

import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JarArtifactHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private JarArtifactHandlerImpl handler = null;

    private JarArtifactHandlerImpl handlerSpy = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new JarArtifactHandlerImpl(mojo);
        handlerSpy = spy(handler);
    }

    @Test
    public void publish() throws Exception {
        final File file = new File("");
        doReturn(file).when(handlerSpy).getJarFile();
        doNothing().when(handlerSpy).assureJarFileExisted(any(File.class));
        doNothing().when(handlerSpy).prepareDeploymentFiles(any(File.class));
        doNothing().when(handlerSpy).uploadDirectoryToFTP();

        handlerSpy.publish();
        verify(handlerSpy).uploadDirectoryToFTP();
        
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
        assertEquals("test.jar", handlerSpy.getJarFile().getName());

        doReturn("").when(mojo).getJarFile();
        doReturn("").when(mojo).getBuildDirectoryAbsolutePath();
        final MavenProject project = mock(MavenProject.class);
        doReturn(project).when(mojo).getProject();
        final Build build = mock(Build.class);
        doReturn(build).when(project).getBuild();
        doReturn("test").when(build).getFinalName();
        assertEquals("test.jar", handlerSpy.getJarFile().getName());
    }

    @Test(expected = MojoExecutionException.class)
    public void assureJarFileExistedWhenFileExtWrong() throws MojoExecutionException {
        handlerSpy.assureJarFileExisted(new File("test.jar"));
    }

    @Test(expected = MojoExecutionException.class)
    public void assureJarFileExistedWhenFileNotExist() throws MojoExecutionException {
        final File fileMock = mock(File.class);

        doReturn("test.jar").when(fileMock).getName();
        doReturn(false).when(fileMock).exists();
        handlerSpy.assureJarFileExisted(fileMock);
    }

    @Test(expected = MojoExecutionException.class)
    public void assureJarFileExistedWhenIsNotAFile() throws MojoExecutionException {
        final File fileMock = mock(File.class);

        doReturn("test.jar").when(fileMock).getName();
        doReturn(false).when(fileMock).exists();
        doReturn(false).when(fileMock).isFile();
        handlerSpy.assureJarFileExisted(fileMock);
    }

    @Test
    public void assureJarFileExisted() throws MojoExecutionException {
        final File file = mock(File.class);
        doReturn("test.jar").when(file).getName();
        doReturn(true).when(file).exists();
        doReturn(true).when(file).isFile();

        handlerSpy.assureJarFileExisted(file);

        verify(file).getName();
        verify(file).exists();
        verify(file).isFile();
    }
}
