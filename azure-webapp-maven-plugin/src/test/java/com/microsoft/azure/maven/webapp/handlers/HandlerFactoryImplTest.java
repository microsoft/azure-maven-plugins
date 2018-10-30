/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.handlers.v1.JarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.NullRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.v1.WarArtifactHandlerImpl;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;

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
    public void getRuntimeHandler() throws Exception {
        final HandlerFactory factory = new HandlerFactoryImpl();

        RuntimeHandler handler;

        // <javaVersion> == null && <containerSettings> == null
        doReturn(null).when(mojo).getJavaVersion();
        doReturn(null).when(mojo).getContainerSettings();

        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof NullRuntimeHandlerImpl);

        // <javaVersion> != null && <containerSettings> == null
        doReturn(JavaVersion.JAVA_8_NEWEST).when(mojo).getJavaVersion();
        doReturn(null).when(mojo).getContainerSettings();

        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof WindowsRuntimeHandlerImpl);

        // set up ContainerSettings
        final ContainerSetting containerSetting = new ContainerSetting();
        containerSetting.setImageName("nginx");

        // <javaVersion> != null && <containerSettings> != null
        doReturn(JavaVersion.JAVA_8_NEWEST).when(mojo).getJavaVersion();
        doReturn(containerSetting).when(mojo).getContainerSettings();
        MojoExecutionException exception = null;
        try {
            factory.getRuntimeHandler(mojo);
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }

        // <javaVersion> == null && Public Docker Container Image
        doReturn(null).when(mojo).getJavaVersion();
        doReturn(containerSetting).when(mojo).getContainerSettings();

        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof PublicDockerHubRuntimeHandlerImpl);

        // <javaVersion> == null && Private Docker Container Image
        doReturn(null).when(mojo).getJavaVersion();
        doReturn(containerSetting).when(mojo).getContainerSettings();
        containerSetting.setServerId("serverId");

        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof PrivateDockerHubRuntimeHandlerImpl);

        // <javaVersion> == null && Private Registry Image
        doReturn(null).when(mojo).getJavaVersion();
        doReturn(containerSetting).when(mojo).getContainerSettings();
        containerSetting.setServerId("serverId");
        containerSetting.setRegistryUrl(new URL("https://microsoft.azurecr.io"));

        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof PrivateRegistryRuntimeHandlerImpl);
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

        assertTrue(factory.getArtifactHandlerFromPackaging(mojo) instanceof JarArtifactHandlerImpl);
    }

    @Test(expected = MojoExecutionException.class)
    public void getArtifactHandlerFromPackagingThrowException() throws MojoExecutionException {
        final MavenProject project = mock(MavenProject.class);
        doReturn(project).when(mojo).getProject();
        doReturn("unknown").when(project).getPackaging();
        final HandlerFactoryImpl factory = new HandlerFactoryImpl();
        factory.getArtifactHandlerFromPackaging(mojo);
    }

    @Test
    public void getV2RuntimeHandler() throws AzureAuthFailureException, MojoExecutionException {
        final HandlerFactory factory = new HandlerFactoryImpl();
        RuntimeHandler handler;
        doReturn("westeurope").when(mojo).getRegion();

        doReturn(SchemaVersion.V2.toString()).when(mojo).getSchemaVersion();
        final RuntimeSetting runtime = mock(RuntimeSetting.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn(JavaVersion.fromString("jre8")).when(runtime).getJavaVersion();
        doReturn("linux").when(runtime).getOs();

        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof LinuxRuntimeHandlerImpl);

        doReturn("windows").when(runtime).getOs();
        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof WindowsRuntimeHandlerImpl);

        doReturn("docker").when(runtime).getOs();
        doReturn("imageName").when(runtime).getImage();
        doReturn("serverId").when(runtime).getServerId();
        doReturn("registry").when(runtime).getRegistryUrl();
        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof PrivateRegistryRuntimeHandlerImpl);

        doReturn("docker").when(runtime).getOs();
        doReturn("imageName").when(runtime).getImage();
        doReturn("serverId").when(runtime).getServerId();
        doReturn("").when(runtime).getRegistryUrl();
        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof PrivateDockerHubRuntimeHandlerImpl);

        doReturn("docker").when(runtime).getOs();
        doReturn("imageName").when(runtime).getImage();
        doReturn("").when(runtime).getServerId();
        doReturn("").when(runtime).getRegistryUrl();
        handler = factory.getRuntimeHandler(mojo);
        assertTrue(handler instanceof PublicDockerHubRuntimeHandlerImpl);
    }

    @Test(expected = MojoExecutionException.class)
    public void regionIsRequiredInV2() throws AzureAuthFailureException, MojoExecutionException {
        doReturn(SchemaVersion.V2.toString()).when(mojo).getSchemaVersion();
        new HandlerFactoryImpl().getRuntimeHandler(mojo);
    }
}
