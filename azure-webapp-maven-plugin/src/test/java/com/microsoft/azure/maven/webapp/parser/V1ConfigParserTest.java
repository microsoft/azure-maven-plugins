/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.webapp.DeployMojo;
import com.microsoft.azure.maven.webapp.validator.V1ConfigurationValidator;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class V1ConfigParserTest {
    DeployMojo deployMojo;
    V1ConfigurationValidator validator;
    V1ConfigParser parser;

    @Before
    public void setUp() throws Exception {
        deployMojo = mock(DeployMojo.class);
        validator = new V1ConfigurationValidator(deployMojo);
        parser = new V1ConfigParser(deployMojo, validator);
    }

    @Test
    public void getWindowsRuntime() throws AzureExecutionException {
        doReturn("1.8").when(deployMojo).getJavaVersion();
        doReturn(WebContainer.TOMCAT_8_5_NEWEST).when(deployMojo).getJavaWebContainer();
        assertEquals(parser.getRuntime(), Runtime.WINDOWS_JAVA8_TOMCAT85);

        doReturn("java11").when(deployMojo).getJavaVersion();
        doReturn(WebContainer.fromString("java 11")).when(deployMojo).getJavaWebContainer();
        assertEquals(parser.getRuntime(), Runtime.WINDOWS_JAVA11);
    }

    @Test()
    public void getLinuxRuntime() throws AzureExecutionException {
        doReturn("JAVA 8-jre8").when(deployMojo).getLinuxRuntime();
        assertEquals(parser.getRuntime(), Runtime.LINUX_JAVA8);

        doReturn("jbosseap 7.2-java8").when(deployMojo).getLinuxRuntime();
        assertEquals(parser.getRuntime(), Runtime.LINUX_JAVA8_JBOSS72);
    }
}
