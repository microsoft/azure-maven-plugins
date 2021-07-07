/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationParserTest {
    @Mock
    protected AbstractWebAppMojo mojo;

    protected ConfigurationParser parser;

    public void buildParser() {
        MockitoAnnotations.initMocks(this);
        parser = new ConfigurationParser(mojo);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        buildParser();
    }

    @Test
    public void getWebAppName() throws AzureExecutionException {
        doReturn("appName").when(mojo).getAppName();
        assertEquals("appName", parser.getAppName());
    }

    @Test
    public void getResourceGroup() throws AzureExecutionException {
        doReturn("resourceGroupName").when(mojo).getResourceGroup();
        assertEquals("resourceGroupName", parser.getResourceGroup());
    }

    @Test
    public void getWebAppConfiguration() throws AzureExecutionException {
        final ConfigurationParser parserSpy = spy(parser);

        doReturn("appName").when(parserSpy).getAppName();
        doReturn("resourceGroupName").when(parserSpy).getResourceGroup();
        final MavenProject project = mock(MavenProject.class);
        final MavenResourcesFiltering filtering = mock(MavenResourcesFiltering.class);
        final MavenSession session = mock(MavenSession.class);
        doReturn(project).when(mojo).getProject();
        doReturn(filtering).when(mojo).getMavenResourcesFiltering();
        doReturn(session).when(mojo).getSession();
        doReturn("test-staging-path").when(mojo).getDeploymentStagingDirectoryPath();
        doReturn("test-build-directory-path").when(mojo).getBuildDirectoryAbsolutePath();
        doReturn("P1v2").when(mojo).getPricingTier();
        doReturn(JavaVersion.JAVA_8).when(parserSpy).getJavaVersion();
        doReturn(WebContainer.TOMCAT_85).when(parserSpy).getWebContainer();
        doReturn(OperatingSystem.WINDOWS).when(parserSpy).getOs();
        doReturn("image").when(parserSpy).getImage();
        WebAppConfiguration webAppConfiguration = parserSpy.getWebAppConfiguration();

        assertEquals("appName", webAppConfiguration.getAppName());
        assertEquals("resourceGroupName", webAppConfiguration.getResourceGroup());
        assertEquals("P1v2", webAppConfiguration.getPricingTier());
        assertEquals(null, webAppConfiguration.getServicePlanName());
        assertEquals(null, webAppConfiguration.getServicePlanResourceGroup());
        assertEquals(OperatingSystem.WINDOWS, webAppConfiguration.getOs());
        assertEquals(null, webAppConfiguration.getMavenSettings());
        assertEquals(project, webAppConfiguration.getProject());
        assertEquals(session, webAppConfiguration.getSession());
        assertEquals(filtering, webAppConfiguration.getFiltering());
        assertEquals("test-staging-path", webAppConfiguration.getStagingDirectoryPath());
        assertEquals("test-build-directory-path", webAppConfiguration.getBuildDirectoryAbsolutePath());

        doReturn(OperatingSystem.LINUX).when(parserSpy).getOs();
        webAppConfiguration = parserSpy.getWebAppConfiguration();
        assertEquals(WebContainer.TOMCAT_85.toString(), webAppConfiguration.getWebContainer());

        doReturn(OperatingSystem.DOCKER).when(parserSpy).getOs();
        webAppConfiguration = parserSpy.getWebAppConfiguration();
        assertEquals("image", webAppConfiguration.getImage());
        assertEquals(null, webAppConfiguration.getServerId());
        assertEquals(null, webAppConfiguration.getRegistryUrl());
    }

    @Test
    public void getOs() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(runtime).when(mojo).getRuntime();

        doReturn(true).when(runtime).isEmpty();
        assertEquals(null, parser.getOs());

        doReturn(false).when(runtime).isEmpty();
        doReturn("windows").when(runtime).getOs();
        assertEquals(OperatingSystem.WINDOWS, parser.getOs());

        doReturn("linux").when(runtime).getOs();
        assertEquals(OperatingSystem.LINUX, parser.getOs());

        doReturn("docker").when(runtime).getOs();
        assertEquals(OperatingSystem.DOCKER, parser.getOs());
    }

    @Test
    public void getRegion() throws AzureExecutionException {
        doReturn(Region.US_WEST.getName()).when(mojo).getRegion();
        assertEquals(Region.US_WEST, parser.getRegion());
    }

    @Test
    public void getImage() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(runtime).when(mojo).getRuntime();

        doReturn("imageName").when(runtime).getImage();
        assertEquals("imageName", parser.getImage());
    }

    @Test
    public void getServerId() {
        assertNull(parser.getServerId());

        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn("serverId").when(runtime).getServerId();
        assertEquals("serverId", parser.getServerId());
    }

    @Test
    public void getRegistryUrl() {
        assertNull(parser.getRegistryUrl());

        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn("serverId").when(runtime).getRegistryUrl();
        assertEquals("serverId", parser.getRegistryUrl());
    }

    @Test
    public void getWebContainer() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn(WebContainer.TOMCAT_85).when(runtime).getWebContainer();
        assertEquals(WebContainer.TOMCAT_85, parser.getWebContainer());
    }

    @Test
    public void getJavaVersion() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn(JavaVersion.JAVA_8).when(runtime).getJavaVersion();
        assertEquals(JavaVersion.JAVA_8, parser.getJavaVersion());
    }

    @Test
    public void getResources() {
        doReturn(null).when(mojo).getDeployment();
        assertNull(parser.getResources());

        final List<Resource> resources = new ArrayList<Resource>();
        resources.add(new Resource());
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(mojo).getDeployment();
        doReturn(resources).when(deployment).getResources();

        assertEquals(resources, parser.getResources());
    }
}
