/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.maven.webapp.DeployMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
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
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class ConfigParserTest {
    DeployMojo deployMojo;
    MavenRuntimeConfig runtimeSetting;
    ConfigParser parser;

    @Before
    public void setUp() {
        deployMojo = mock(DeployMojo.class);
        runtimeSetting = mock(MavenRuntimeConfig.class);
        doReturn(runtimeSetting).when(deployMojo).getRuntime();
        parser = new ConfigParser(deployMojo);
    }

    @Test
    public void getWebAppConfiguration() throws AzureExecutionException {
        final ConfigParser parserSpy = spy(parser);

        doReturn("appName").when(parserSpy).getAppName();
        doReturn("resourceGroupName").when(parserSpy).getResourceGroup();
        final MavenProject project = mock(MavenProject.class);
        final MavenResourcesFiltering filtering = mock(MavenResourcesFiltering.class);
        final MavenSession session = mock(MavenSession.class);
        doReturn(project).when(deployMojo).getProject();
        doReturn(filtering).when(deployMojo).getMavenResourcesFiltering();
        doReturn(session).when(deployMojo).getSession();
        doReturn("test-staging-path").when(deployMojo).getDeploymentStagingDirectoryPath();
        doReturn("test-build-directory-path").when(deployMojo).getBuildDirectoryAbsolutePath();
        doReturn("P1v2").when(deployMojo).getPricingTier();
        final Runtime mockRuntime = Runtime.getRuntime(OperatingSystem.WINDOWS, WebContainer.TOMCAT_85, JavaVersion.JAVA_8);
        doReturn(mockRuntime).when(parserSpy).getRuntime();
        final List<Resource> resources = new ArrayList<>();
        resources.add(new Resource());
        final Deployment deployment = mock(Deployment.class);
        doReturn(deployment).when(deployMojo).getDeployment();
        doReturn(resources).when(deployment).getResources();
        doReturn(Region.US_WEST.getName()).when(deployMojo).getRegion();

        final WebAppConfiguration webAppConfiguration = parserSpy.getWebAppConfiguration();

        assertEquals(Region.US_WEST, parser.getRegion());
        assertEquals(resources, webAppConfiguration.getResources());
        assertEquals("appName", webAppConfiguration.getAppName());
        assertEquals("resourceGroupName", webAppConfiguration.getResourceGroup());
        assertEquals("P1v2", webAppConfiguration.getPricingTier());
        assertNull(webAppConfiguration.getServicePlanName());
        assertNull(webAppConfiguration.getServicePlanResourceGroup());
        assertEquals(OperatingSystem.WINDOWS, webAppConfiguration.getOs());
        assertNull(webAppConfiguration.getMavenSettings());
        assertEquals(project, webAppConfiguration.getProject());
        assertEquals(session, webAppConfiguration.getSession());
        assertEquals(filtering, webAppConfiguration.getFiltering());
        assertEquals("test-staging-path", webAppConfiguration.getStagingDirectoryPath());
        assertEquals("test-build-directory-path", webAppConfiguration.getBuildDirectoryAbsolutePath());
    }

    @Test
    public void getWindowsRuntime() {
        doReturn("windows").when(runtimeSetting).getOs();
        doReturn("Java 8").when(runtimeSetting).getJavaVersion();
        doReturn("Java SE").when(runtimeSetting).getWebContainer();
        assertEquals(parser.getRuntime(), Runtime.WINDOWS_JAVA8);

        doReturn("windows").when(runtimeSetting).getOs();
        doReturn("Java 11").when(runtimeSetting).getJavaVersion();
        doReturn("tomcat 8.5").when(runtimeSetting).getWebContainer();
        assertEquals(parser.getRuntime(), Runtime.WINDOWS_JAVA11_TOMCAT85);
    }

    @Test()
    public void getLinuxRuntime() {
        doReturn("linux").when(runtimeSetting).getOs();
        doReturn("Java 8").when(runtimeSetting).getJavaVersion();
        doReturn("JBosseap 7").when(runtimeSetting).getWebContainer();
        assertEquals(parser.getRuntime(), Runtime.LINUX_JAVA8_JBOSS7);

        doReturn("linux").when(runtimeSetting).getOs();
        doReturn("Java 11").when(runtimeSetting).getJavaVersion();
        doReturn("JAVA SE").when(runtimeSetting).getWebContainer();
        assertEquals(parser.getRuntime(), Runtime.LINUX_JAVA11);
    }

    @Test
    public void getPricingTier() {
        // basic
        doReturn("b1").when(deployMojo).getPricingTier();
        assertEquals(PricingTier.BASIC_B1, parser.getPricingTier());
        // standard
        doReturn("S2").when(deployMojo).getPricingTier();
        assertEquals(PricingTier.STANDARD_S2, parser.getPricingTier());
        // premium
        doReturn("p3").when(deployMojo).getPricingTier();
        assertEquals(PricingTier.PREMIUM_P3, parser.getPricingTier());
        // premium v2
        doReturn("P1v2").when(deployMojo).getPricingTier();
        assertEquals(PricingTier.PREMIUM_P1V2, parser.getPricingTier());
        // premium v3
        doReturn("P3V3").when(deployMojo).getPricingTier();
        assertEquals(PricingTier.PREMIUM_P3V3, parser.getPricingTier());
    }
}
