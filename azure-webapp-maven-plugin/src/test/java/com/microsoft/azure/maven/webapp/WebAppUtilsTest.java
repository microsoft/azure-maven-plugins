/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class WebAppUtilsTest {
    @Test
    public void assureLinuxWebApp() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();

        // Linux Web App
        WebAppUtils.assureLinuxWebApp(app);

        // Non-Linux Web App
        doReturn("app").when(siteInner).kind();
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
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app").when(siteInner).kind();
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();

        // Windows Web App
        WebAppUtils.assureWindowsWebApp(app);

        // Linux Web App
        doReturn("app,linux").when(siteInner).kind();
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
    public void getDockerImageType() throws Exception {
        final ContainerSetting containerSetting = new ContainerSetting();
        assertEquals(DockerImageType.NONE, WebAppUtils.getDockerImageType(containerSetting));

        containerSetting.setImageName("imageName");
        assertEquals(DockerImageType.PUBLIC_DOCKER_HUB, WebAppUtils.getDockerImageType(containerSetting));

        containerSetting.setUseBuiltInImage(true);
        assertEquals(DockerImageType.BUILT_IN, WebAppUtils.getDockerImageType(containerSetting));
        containerSetting.setUseBuiltInImage(false);

        containerSetting.setServerId("serverId");
        assertEquals(DockerImageType.PRIVATE_DOCKER_HUB, WebAppUtils.getDockerImageType(containerSetting));

        containerSetting.setRegistryUrl(new URL("https://microsoft.azurecr.io"));
        assertEquals(DockerImageType.PRIVATE_REGISTRY, WebAppUtils.getDockerImageType(containerSetting));

        containerSetting.setServerId("");
        assertEquals(DockerImageType.UNKNOWN, WebAppUtils.getDockerImageType(containerSetting));
    }
}
