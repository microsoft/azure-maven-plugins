/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

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
        assertTrue(handler instanceof JavaRuntimeHandlerImpl);

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
}
