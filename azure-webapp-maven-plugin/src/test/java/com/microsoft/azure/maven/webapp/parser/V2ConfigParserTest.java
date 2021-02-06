/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.webapp.DeployMojo;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.maven.webapp.validator.V2ConfigurationValidator;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class V2ConfigParserTest {
    DeployMojo deployMojo;
    MavenRuntimeConfig runtimeSetting;
    V2ConfigurationValidator validator;
    V2ConfigParser parser;

    @Before
    public void setUp() throws Exception {
        deployMojo = mock(DeployMojo.class);
        runtimeSetting = mock(MavenRuntimeConfig.class);
        doReturn(runtimeSetting).when(deployMojo).getRuntime();
        validator = new V2ConfigurationValidator(deployMojo);
        parser = new V2ConfigParser(deployMojo, validator);
    }

    @Test
    public void getWindowsRuntime() throws AzureExecutionException {
        doReturn("windows").when(runtimeSetting).getOs();
        doReturn("Java 8").when(runtimeSetting).getJavaVersionRaw();
        doReturn("Java SE").when(runtimeSetting).getWebContainerRaw();
        assertEquals(parser.getRuntime(), Runtime.WINDOWS_JAVA8);

        doReturn("windows").when(runtimeSetting).getOs();
        doReturn("Java 11").when(runtimeSetting).getJavaVersionRaw();
        doReturn("tomcat 8.5").when(runtimeSetting).getWebContainerRaw();
        assertEquals(parser.getRuntime(), Runtime.WINDOWS_JAVA11_TOMCAT85);

        doReturn("windows").when(runtimeSetting).getOs();
        doReturn("11.0.2_ZULU").when(runtimeSetting).getJavaVersionRaw();
        doReturn("tomcat 8.5").when(runtimeSetting).getWebContainerRaw();
        assertEquals(parser.getRuntime().getJavaVersion(), JavaVersion.JAVA_ZULU_11_0_2);
    }

    @Test()
    public void getLinuxRuntime() throws AzureExecutionException {
        doReturn("linux").when(runtimeSetting).getOs();
        doReturn("Java 8").when(runtimeSetting).getJavaVersionRaw();
        doReturn("JBosseap 7.2").when(runtimeSetting).getWebContainerRaw();
        assertEquals(parser.getRuntime(), Runtime.LINUX_JAVA8_JBOSS72);

        doReturn("linux").when(runtimeSetting).getOs();
        doReturn("Java 11").when(runtimeSetting).getJavaVersionRaw();
        doReturn("JAVA SE").when(runtimeSetting).getWebContainerRaw();
        assertEquals(parser.getRuntime(), Runtime.LINUX_JAVA11);
    }

    @Test(expected = AzureExecutionException.class)
    public void wrongLinuxRuntime() throws AzureExecutionException {
        doReturn("linux").when(runtimeSetting).getOs();
        doReturn("Java 11").when(runtimeSetting).getJavaVersionRaw();
        doReturn("JBosseap 7.2").when(runtimeSetting).getWebContainerRaw();
        parser.getRuntime();
    }
}
