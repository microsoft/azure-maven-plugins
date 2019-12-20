/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.appservice.OperatingSystemEnum;
import com.microsoft.azure.maven.handlers.ArtifactHandler;
import com.microsoft.azure.maven.handlers.RuntimeHandler;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.handlers.artifact.JarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.artifact.WarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.LinuxRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.NullRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.PrivateDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.PrivateRegistryRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.PublicDockerHubRuntimeHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.runtime.WindowsRuntimeHandlerImpl;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
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
    public void getRuntimeHandler() throws AzureExecutionException {
        final WebAppConfiguration config = mock(WebAppConfiguration.class);
        final Azure azureClient = mock(Azure.class);
        final Log log = mock(Log.class);
        final HandlerFactory factory = new HandlerFactoryImpl();

        doReturn("").when(config).getAppName();
        doReturn("").when(config).getResourceGroup();
        doReturn(Region.US_EAST).when(config).getRegion();
        doReturn(new PricingTier("Premium", "P1V2")).when(config).getPricingTier();
        doReturn("").when(config).getServicePlanName();
        doReturn("").when(config).getServicePlanResourceGroup();

        doReturn(null).when(config).getOs();
        RuntimeHandler handler = factory.getRuntimeHandler(config, azureClient, log);
        assertTrue(handler instanceof NullRuntimeHandlerImpl);

        doReturn(OperatingSystemEnum.Windows).when(config).getOs();
        doReturn(JavaVersion.JAVA_8_NEWEST).when(config).getJavaVersion();
        doReturn(WebContainer.TOMCAT_8_5_NEWEST).when(config).getWebContainer();
        handler = factory.getRuntimeHandler(config, azureClient, log);
        assertTrue(handler instanceof WindowsRuntimeHandlerImpl);

        doReturn(OperatingSystemEnum.Linux).when(config).getOs();
        doReturn(RuntimeStack.JAVA_8_JRE8).when(config).getRuntimeStack();
        handler = factory.getRuntimeHandler(config, azureClient, log);
        assertTrue(handler instanceof LinuxRuntimeHandlerImpl);

        doReturn(OperatingSystemEnum.Docker).when(config).getOs();
        doReturn("imageName").when(config).getImage();
        handler = factory.getRuntimeHandler(config, azureClient, log);
        assertTrue(handler instanceof PublicDockerHubRuntimeHandlerImpl);

        doReturn("serverId").when(config).getServerId();
        doReturn("registry").when(config).getRegistryUrl();
        handler = factory.getRuntimeHandler(config, azureClient, log);
        assertTrue(handler instanceof PrivateRegistryRuntimeHandlerImpl);

        doReturn("").when(config).getRegistryUrl();
        handler = factory.getRuntimeHandler(config, azureClient, log);
        assertTrue(handler instanceof PrivateDockerHubRuntimeHandlerImpl);

        doReturn("").when(config).getImage();
        try {
            factory.getRuntimeHandler(config, azureClient, log);
        } catch (AzureExecutionException e) {
            assertEquals(e.getMessage(), "Invalid docker runtime configured.");
        }
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
    public void getArtifactHandlerFromPackaging() throws AzureExecutionException {
        final MavenProject project = mock(MavenProject.class);
        doReturn(project).when(mojo).getProject();
        doReturn("jar").when(project).getPackaging();

        final HandlerFactoryImpl factory = new HandlerFactoryImpl();

        assertTrue(factory.getArtifactHandlerBuilderFromPackaging(mojo).build() instanceof JarArtifactHandlerImpl);
    }

    @Test(expected = AzureExecutionException.class)
    public void getArtifactHandlerFromPackagingThrowException() throws AzureExecutionException {
        final MavenProject project = mock(MavenProject.class);
        doReturn(project).when(mojo).getProject();
        doReturn("unknown").when(project).getPackaging();
        final HandlerFactoryImpl factory = new HandlerFactoryImpl();
        factory.getArtifactHandlerBuilderFromPackaging(mojo);
    }
}
