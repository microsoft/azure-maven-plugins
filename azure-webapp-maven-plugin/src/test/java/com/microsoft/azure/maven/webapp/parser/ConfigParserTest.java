/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.maven.webapp.DeployMojo;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ConfigParserTest {
    DeployMojo deployMojo;
    MavenRuntimeConfig runtimeSetting;
    ConfigParser parser;

    @Before
    public void setUp() throws Exception {
        deployMojo = mock(DeployMojo.class);
        runtimeSetting = mock(MavenRuntimeConfig.class);
        doReturn(runtimeSetting).when(deployMojo).getRuntime();
        parser = new ConfigParser(deployMojo);
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

    @Test
    public void getPricingTier() throws AzureExecutionException {
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
