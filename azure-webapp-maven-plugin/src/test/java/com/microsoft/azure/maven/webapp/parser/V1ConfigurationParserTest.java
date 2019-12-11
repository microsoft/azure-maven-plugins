/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.appservice.OperatingSystemEnum;
import com.microsoft.azure.maven.webapp.validator.V1ConfigurationValidator;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class V1ConfigurationParserTest {
    @Mock
    protected AbstractWebAppMojo mojo;

    protected V1ConfigurationParser parser;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        parser = new V1ConfigurationParser(mojo, new V1ConfigurationValidator(mojo));
    }

    @Test
    public void getOs() throws MojoExecutionException {
        assertNull(parser.getOs());

        doReturn("1.8").when(mojo).getJavaVersion();
        assertEquals(OperatingSystemEnum.Windows, parser.getOs());

        doReturn(null).when(mojo).getJavaVersion();
        doReturn("linuxRuntime").when(mojo).getLinuxRuntime();
        assertEquals(OperatingSystemEnum.Linux, parser.getOs());

        doReturn(null).when(mojo).getJavaVersion();
        doReturn(null).when(mojo).getLinuxRuntime();
        doReturn(mock(ContainerSetting.class)).when(mojo).getContainerSettings();
        assertEquals(OperatingSystemEnum.Docker, parser.getOs());

        doReturn("linuxRuntime").when(mojo).getLinuxRuntime();
        doReturn("1.8").when(mojo).getJavaVersion();
        try {
            parser.getOs();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "Conflict settings found. <javaVersion>, <linuxRuntime>" +
                "and <containerSettings> should not be set at the same time.");
        }
    }

    @Test
    public void getRegion() throws MojoExecutionException {
        assertEquals(Region.EUROPE_WEST, parser.getRegion());

        doReturn("unknown-region").when(mojo).getRegion();
        try {
            parser.getRegion();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "The value of <region> is not correct, please correct it in pom.xml.");
        }

        doReturn(Region.US_WEST.name()).when(mojo).getRegion();
        assertEquals(Region.US_WEST, parser.getRegion());
    }

    @Test
    public void getRuntimeStack() throws MojoExecutionException {
        try {
            parser.getRuntimeStack();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "Please configure the <linuxRuntime> in pom.xml.");
        }

        doReturn("tomcat 8.5-jre8").when(mojo).getLinuxRuntime();
        assertEquals(RuntimeStack.TOMCAT_8_5_JRE8, parser.getRuntimeStack());

        doReturn("tomcat 9.0-jre8").when(mojo).getLinuxRuntime();
        assertEquals(RuntimeStack.TOMCAT_9_0_JRE8, parser.getRuntimeStack());

        doReturn("wildfly 14-jre8").when(mojo).getLinuxRuntime();
        assertEquals(RuntimeStack.WILDFLY_14_JRE8, parser.getRuntimeStack());

        doReturn("jre8").when(mojo).getLinuxRuntime();
        assertEquals(RuntimeStack.JAVA_8_JRE8, parser.getRuntimeStack());

        doReturn("tomcat 8.5-java11").when(mojo).getLinuxRuntime();
        assertEquals(RuntimeStack.TOMCAT_8_5_JAVA11, parser.getRuntimeStack());

        doReturn("tomcat 9.0-java11").when(mojo).getLinuxRuntime();
        assertEquals(RuntimeStack.TOMCAT_9_0_JAVA11, parser.getRuntimeStack());

        doReturn("java11").when(mojo).getLinuxRuntime();
        assertEquals(RuntimeStack.JAVA_11_JAVA11, parser.getRuntimeStack());

        doReturn("unknown-value").when(mojo).getLinuxRuntime();
        try {
            parser.getRuntimeStack();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "The configuration of <linuxRuntime> in pom.xml is not correct. " +
                "Please refer https://aka.ms/maven_webapp_runtime_v1 for more information");
        }
    }

    @Test
    public void getImage() throws MojoExecutionException {
        try {
            parser.getImage();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "Please config the <containerSettings> in pom.xml.");
        }

        final ContainerSetting containerSetting = mock(ContainerSetting.class);
        doReturn(containerSetting).when(mojo).getContainerSettings();
        doReturn("imageName").when(containerSetting).getImageName();
        assertEquals("imageName", parser.getImage());

        doReturn("").when(containerSetting).getImageName();
        try {
            parser.getImage();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "Please config the <imageName> of <containerSettings> in pom.xml.");
        }
    }

    @Test
    public void getServerId() {
        assertNull(parser.getServerId());

        final ContainerSetting containerSetting = mock(ContainerSetting.class);
        doReturn(containerSetting).when(mojo).getContainerSettings();
        doReturn("serverId").when(containerSetting).getServerId();
        assertEquals("serverId", parser.getServerId());
    }

    @Test
    public void getRegistryUrl() {
        assertNull(parser.getRegistryUrl());

        final ContainerSetting containerSetting = mock(ContainerSetting.class);
        doReturn(containerSetting).when(mojo).getContainerSettings();
        doReturn("registryUrl").when(containerSetting).getRegistryUrl();
        assertEquals("registryUrl", parser.getRegistryUrl());
    }

    @Test
    public void getWebContainer() throws MojoExecutionException {
        try {
            parser.getWebContainer();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "The configuration of <javaWebContainer> in pom.xml is not correct.");
        }

        doReturn(WebContainer.TOMCAT_8_5_NEWEST).when(mojo).getJavaWebContainer();
        assertEquals(WebContainer.TOMCAT_8_5_NEWEST, parser.getWebContainer());
    }

    @Test
    public void getJavaVersion() throws MojoExecutionException {
        try {
            parser.getJavaVersion();
        } catch (MojoExecutionException e) {
            assertEquals(e.getMessage(), "Please config the <javaVersion> in pom.xml.");
        }

        doReturn("1.8").when(mojo).getJavaVersion();
        assertEquals(JavaVersion.JAVA_8_NEWEST, parser.getJavaVersion());
    }
}
