/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WebAppUtilsTest {
    @Test
    public void assureLinuxWebApp() throws Exception {
        final WebApp app = mock(WebApp.class);
        final SiteInner siteInner = mock(SiteInner.class);
        when(app.inner()).thenReturn(siteInner);
        when(siteInner.kind()).thenReturn("app,linux");

        // Linux Web App
        WebAppUtils.assureLinuxWebApp(app);

        // Non-Linux Web App
        when(siteInner.kind()).thenReturn("app");
        MojoExecutionException exception = null;
        try {
            WebAppUtils.assureLinuxWebApp(app);
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }
    }

    @Test
    public void assureWindowsWebApp() throws Exception {
        final WebApp app = mock(WebApp.class);
        final SiteInner siteInner = mock(SiteInner.class);
        when(app.inner()).thenReturn(siteInner);
        when(siteInner.kind()).thenReturn("app");

        // Windows Web App
        WebAppUtils.assureWindowsWebApp(app);

        // Linux Web App
        when(siteInner.kind()).thenReturn("app,linux");
        MojoExecutionException exception = null;
        try {
            WebAppUtils.assureWindowsWebApp(app);
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }
    }

    @Test
    public void isPublicDockerHubImage() throws Exception {
        final ContainerSetting containerSetting = new ContainerSetting();
        assertTrue(WebAppUtils.isPublicDockerHubImage(containerSetting));

        containerSetting.setServerId("serverId");
        assertFalse(WebAppUtils.isPublicDockerHubImage(containerSetting));

        containerSetting.setRegistryUrl(new URL("https://microsoft.azurecr.io"));
        assertFalse(WebAppUtils.isPublicDockerHubImage(containerSetting));
    }

    @Test
    public void isPrivateDockerHubImage() throws Exception {
        final ContainerSetting containerSetting = new ContainerSetting();
        assertFalse(WebAppUtils.isPrivateDockerHubImage(containerSetting));

        containerSetting.setServerId("serverId");
        assertTrue(WebAppUtils.isPrivateDockerHubImage(containerSetting));

        containerSetting.setRegistryUrl(new URL("https://microsoft.azurecr.io"));
        assertFalse(WebAppUtils.isPrivateDockerHubImage(containerSetting));
    }

    @Test
    public void isPrivateRegistryImage() throws Exception {
        final ContainerSetting containerSetting = new ContainerSetting();
        assertFalse(WebAppUtils.isPrivateRegistryImage(containerSetting));

        containerSetting.setServerId("serverId");
        assertFalse(WebAppUtils.isPrivateRegistryImage(containerSetting));

        containerSetting.setRegistryUrl(new URL("https://microsoft.azurecr.io"));
        assertTrue(WebAppUtils.isPrivateRegistryImage(containerSetting));
    }

}
