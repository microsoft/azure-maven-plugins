/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.maven.webapp.validator.V2ConfigurationValidator;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class V2ConfigurationParserTest {
    @Mock
    protected AbstractWebAppMojo mojo;

    protected V2ConfigurationParser parser;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        parser = new V2ConfigurationParser(mojo, new V2ConfigurationValidator(mojo));
    }

    @Test
    public void getOs() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(runtime).when(mojo).getRuntime();

        doReturn(true).when(runtime).isEmpty();
        assertEquals(null, parser.getOs());

        doReturn(false).when(runtime).isEmpty();
        doReturn("windows").when(runtime).getOs();
        assertEquals(OperatingSystemEnum.Windows, parser.getOs());

        doReturn("linux").when(runtime).getOs();
        assertEquals(OperatingSystemEnum.Linux, parser.getOs());

        doReturn("docker").when(runtime).getOs();
        assertEquals(OperatingSystemEnum.Docker, parser.getOs());

        try {
            doReturn(null).when(runtime).getOs();
            parser.getOs();
        } catch (AzureExecutionException e) {
            assertEquals(e.getMessage(), "Pleas configure the <os> of <runtime> in pom.xml.");
        }

        try {
            doReturn("unknown-os").when(runtime).getOs();
            parser.getOs();
        } catch (AzureExecutionException e) {
            assertEquals(e.getMessage(), "The value of <os> is not correct, supported values are: windows, " +
                "linux and docker.");
        }
    }

    @Test
    public void getRegion() throws AzureExecutionException {
        doReturn("unknown-region").when(mojo).getRegion();
        try {
            parser.getRegion();
        } catch (AzureExecutionException e) {
            assertEquals(e.getMessage(), "The value of <region> is not supported, please correct it in pom.xml.");
        }

        doReturn(Region.US_WEST.name()).when(mojo).getRegion();
        assertEquals(Region.US_WEST, parser.getRegion());
    }

    @Test
    public void getRuntimeStack() throws AzureExecutionException {
        doReturn(null).when(mojo).getRuntime();
        assertNull(parser.getRuntimeStack());

        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(true).when(runtime).isEmpty();
        doReturn(runtime).when(mojo).getRuntime();
        assertNull(parser.getRuntimeStack());

        doReturn(false).when(runtime).isEmpty();
        doReturn(RuntimeStack.TOMCAT_8_5_JRE8).when(runtime).getLinuxRuntime();
        assertEquals(RuntimeStack.TOMCAT_8_5_JRE8, parser.getRuntimeStack());

        doReturn(RuntimeStack.TOMCAT_9_0_JRE8).when(runtime).getLinuxRuntime();
        assertEquals(RuntimeStack.TOMCAT_9_0_JRE8, parser.getRuntimeStack());

        doReturn(RuntimeStack.JAVA_8_JRE8).when(runtime).getLinuxRuntime();
        assertEquals(RuntimeStack.JAVA_8_JRE8, parser.getRuntimeStack());
    }

    @Test
    public void getImage() throws AzureExecutionException {
        doReturn(null).when(mojo).getRuntime();
        try {
            parser.getImage();
        } catch (AzureExecutionException e) {
            assertEquals(e.getMessage(), "Please configure the <runtime> in pom.xml.");
        }

        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn("").when(runtime).getImage();
        doReturn(runtime).when(mojo).getRuntime();

        try {
            parser.getImage();
        } catch (AzureExecutionException e) {
            assertEquals(e.getMessage(), "Please config the <image> of <runtime> in pom.xml.");
        }

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
        doReturn(null).when(mojo).getRuntime();

        try {
            parser.getWebContainer();
        } catch (AzureExecutionException e) {
            assertEquals(e.getMessage(), "Please config the <runtime> in pom.xml.");
        }
        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn("windows").when(runtime).getOs();
        doReturn(null).when(runtime).getWebContainer();
        try {
            parser.getWebContainer();
        } catch (AzureExecutionException e) {
            assertTrue(StringUtils.contains(e.getMessage(), " for <webContainer> in pom.xml"));
        }

        doReturn(WebContainer.TOMCAT_8_5_NEWEST).when(runtime).getWebContainer();
        doReturn(WebContainer.TOMCAT_8_5_NEWEST.toString()).when(runtime).getWebContainerRaw();
        assertEquals(WebContainer.TOMCAT_8_5_NEWEST, parser.getWebContainer());
    }

    @Test
    public void getJavaVersion() throws AzureExecutionException {
        doReturn(null).when(mojo).getRuntime();

        try {
            parser.getJavaVersion();
        } catch (AzureExecutionException e) {
            assertEquals(e.getMessage(), "Please config the <runtime> in pom.xml.");
        }
        final MavenRuntimeConfig runtime = mock(MavenRuntimeConfig.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn(null).when(runtime).getJavaVersion();
        try {
            parser.getJavaVersion();
        } catch (AzureExecutionException e) {
            assertTrue(StringUtils.contains(e.getMessage(), " for <javaVersion> in pom.xml"));
        }
        doReturn(JavaVersion.JAVA_8_NEWEST).when(runtime).getJavaVersion();
        doReturn(JavaVersion.JAVA_8_NEWEST.toString()).when(runtime).getJavaVersionRaw();
        assertEquals(JavaVersion.JAVA_8_NEWEST, parser.getJavaVersion());
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
