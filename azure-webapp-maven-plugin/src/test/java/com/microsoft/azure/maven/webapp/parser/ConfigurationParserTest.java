/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationParserTest {
    @Mock
    protected AbstractWebAppMojo mojo;

    protected ConfigurationParser parser;

    public void buildParser() {
        parser = new ConfigurationParser(mojo) {
            @Override
            protected OperatingSystemEnum getOs() {
                return null;
            }

            @Override
            protected Region getRegion() {
                return Region.EUROPE_WEST;
            }

            @Override
            protected RuntimeStack getRuntimeStack() {
                return RuntimeStack.TOMCAT_8_5_JRE8;
            }

            @Override
            protected String getImage() {
                return "image";
            }

            @Override
            protected String getServerId() {
                return null;
            }

            @Override
            protected String getRegistryUrl() {
                return null;
            }

            @Override
            protected String getSchemaVersion() {
                return null;
            }

            @Override
            protected JavaVersion getJavaVersion() {
                return JavaVersion.JAVA_8_NEWEST;
            }

            @Override
            protected WebContainer getWebContainer() {
                return WebContainer.TOMCAT_8_5_NEWEST;
            }

            @Override
            protected List<Resource> getResources() {
                return null;
            }
        };
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        buildParser();
    }

    @Test
    public void getWebAppName() throws MojoExecutionException {
        doReturn("appName").when(mojo).getAppName();
        assertEquals("appName", parser.getAppName());

        doReturn("-invalidAppName").when(mojo).getAppName();
        try {
            parser.getAppName();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "The <appName> only allow alphanumeric characters, " +
                "hyphens and cannot start or end in a hyphen.");
        }

        doReturn(null).when(mojo).getAppName();
        try {
            parser.getAppName();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "Please config the <appName> in pom.xml.");
        }
    }

    @Test
    public void getResourceGroup() throws MojoExecutionException {
        doReturn("resourceGroupName").when(mojo).getResourceGroup();
        assertEquals("resourceGroupName", parser.getResourceGroup());

        doReturn("invalid**ResourceGroupName").when(mojo).getResourceGroup();
        try {
            parser.getResourceGroup();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "The <resourceGroup> only allow alphanumeric characters, periods, " +
                "underscores, hyphens and parenthesis and cannot end in a period.");
        }

        doReturn(null).when(mojo).getResourceGroup();
        try {
            parser.getResourceGroup();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "Please config the <resourceGroup> in pom.xml.");
        }
    }

    @Test
    public void getWebAppConfiguration() throws MojoExecutionException {
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
        doReturn(new PricingTier("Premium", "P1V2")).when(mojo).getPricingTier();

        doReturn(OperatingSystemEnum.Windows).when(parserSpy).getOs();
        WebAppConfiguration webAppConfiguration = parserSpy.getWebAppConfiguration();

        assertEquals("appName", webAppConfiguration.getAppName());
        assertEquals("resourceGroupName", webAppConfiguration.getResourceGroup());
        assertEquals(Region.EUROPE_WEST, webAppConfiguration.getRegion());
        assertEquals(new PricingTier("Premium", "P1V2"), webAppConfiguration.getPricingTier());
        assertEquals(null, webAppConfiguration.getServicePlanName());
        assertEquals(null, webAppConfiguration.getServicePlanResourceGroup());
        assertEquals(OperatingSystemEnum.Windows, webAppConfiguration.getOs());
        assertEquals(null, webAppConfiguration.getMavenSettings());
        assertEquals(project, webAppConfiguration.getProject());
        assertEquals(session, webAppConfiguration.getSession());
        assertEquals(filtering, webAppConfiguration.getFiltering());
        assertEquals("test-staging-path", webAppConfiguration.getStagingDirectoryPath());
        assertEquals("test-build-directory-path", webAppConfiguration.getBuildDirectoryAbsolutePath());

        assertEquals(JavaVersion.JAVA_8_NEWEST, webAppConfiguration.getJavaVersion());
        assertEquals(WebContainer.TOMCAT_8_5_NEWEST, webAppConfiguration.getWebContainer());

        doReturn(OperatingSystemEnum.Linux).when(parserSpy).getOs();
        webAppConfiguration = parserSpy.getWebAppConfiguration();
        assertEquals(RuntimeStack.TOMCAT_8_5_JRE8, webAppConfiguration.getRuntimeStack());

        doReturn(OperatingSystemEnum.Docker).when(parserSpy).getOs();
        webAppConfiguration = parserSpy.getWebAppConfiguration();
        assertEquals("image", webAppConfiguration.getImage());
        assertEquals(null, webAppConfiguration.getServerId());
        assertEquals(null, webAppConfiguration.getRegistryUrl());
    }
}
