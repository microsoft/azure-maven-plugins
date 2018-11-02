/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class HandlerFactoryImplTest {
    @Mock
    AbstractWebAppMojo mojo;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getSettingsHandler() throws Exception {
        final HandlerFactory factory = new HandlerFactoryImpl();

        final SettingsHandler handler = factory.getSettingsHandler(mojo);
        assertNotNull(handler);
        assertTrue(handler instanceof SettingsHandlerImpl);
    }

    @Test
    public void getDefaultArtifactHandler() throws Exception {
        final MavenProject project = mock(MavenProject.class);
        doReturn(project).when(mojo).getProject();
        doReturn(DeploymentType.EMPTY).when(mojo).getDeploymentType();
        doReturn("jar").when(project).getPackaging();

        final HandlerFactory factory = new HandlerFactoryImpl();
        final ArtifactHandler handler = factory.getArtifactHandler(mojo);
        assertTrue(handler instanceof JarArtifactHandlerImpl);
    }

    @Test
    public void getAutoArtifactHandler() throws Exception {
        final MavenProject project = mock(MavenProject.class);
        doReturn(project).when(mojo).getProject();
        doReturn(DeploymentType.AUTO).when(mojo).getDeploymentType();
        doReturn("war").when(project).getPackaging();

        final HandlerFactory factory = new HandlerFactoryImpl();
        final ArtifactHandler handler = factory.getArtifactHandler(mojo);
        assertTrue(handler instanceof WarArtifactHandlerImpl);
    }

    @Test
    public void getDeploymentSlotHandler() throws Exception {
        final HandlerFactory factory = new HandlerFactoryImpl();

        final DeploymentSlotHandler handler = factory.getDeploymentSlotHandler(mojo);
        assertNotNull(handler);
        assertTrue(handler instanceof DeploymentSlotHandler);
    }

    @Test
    public void getArtifactHandlerFromPackaging() throws MojoExecutionException {
        final MavenProject project = mock(MavenProject.class);
        doReturn(project).when(mojo).getProject();
        doReturn("jar").when(project).getPackaging();

        final HandlerFactoryImpl factory = new HandlerFactoryImpl();

        assertTrue(factory.getArtifactHandlerBuilderFromPackaging(mojo).build() instanceof JarArtifactHandlerImpl);
    }

    @Test(expected = MojoExecutionException.class)
    public void getArtifactHandlerFromPackagingThrowException() throws MojoExecutionException {
        final MavenProject project = mock(MavenProject.class);
        doReturn(project).when(mojo).getProject();
        doReturn("unknown").when(project).getPackaging();
        final HandlerFactoryImpl factory = new HandlerFactoryImpl();
        factory.getArtifactHandlerBuilderFromPackaging(mojo);
    }
}
