/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.handlers.*;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployFacadeBaseImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    @Mock
    private Log log;

    private DeployFacadeBaseImpl facade = null;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mojo.getLog()).thenReturn(log);
        facade = new DeployFacadeBaseImpl(mojo) {
            @Override
            public DeployFacade setupRuntime() throws MojoExecutionException {
                return this;
            }

            @Override
            public DeployFacade applySettings() throws MojoExecutionException {
                return this;
            }

            @Override
            public DeployFacade commitChanges() throws MojoExecutionException {
                return this;
            }
        };
    }

    @Test
    public void deployArtifacts() throws Exception {
        // No resources
        when(mojo.getResources()).thenReturn(null);

        facade.deployArtifacts();

        verify(log, times(1)).info(any(String.class));

        // Valid resources
        final ArtifactHandler handler = mock(ArtifactHandler.class);
        final DeployFacadeBaseImpl facadeSpy = spy(facade);
        doReturn(handler).when(facadeSpy).getArtifactHandler();
        when(mojo.getResources()).thenReturn(getResourceList());

        facadeSpy.deployArtifacts();

        verify(handler, times(1)).publish(ArgumentMatchers.<Resource>anyList());
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void getMojo() throws Exception {
        assertSame(mojo, facade.getMojo());
    }

    @Test
    public void getRuntimeHandler() throws Exception {
        RuntimeHandler handler;

        // <javaVersion> == null && <containerSettings> == null
        when(mojo.getJavaVersion()).thenReturn(null);
        when(mojo.getContainerSettings()).thenReturn(null);

        handler = facade.getRuntimeHandler();
        assertTrue(handler instanceof NullRuntimeHandlerImpl);

        // <javaVersion> != null && <containerSettings> == null
        when(mojo.getJavaVersion()).thenReturn(JavaVersion.JAVA_8_NEWEST);
        when(mojo.getContainerSettings()).thenReturn(null);

        handler = facade.getRuntimeHandler();
        assertTrue(handler instanceof JavaRuntimeHandlerImpl);

        // set up ContainerSettings
        final ContainerSetting containerSetting = new ContainerSetting();
        containerSetting.setImageName("nginx");

        // <javaVersion> != null && <containerSettings> != null
        when(mojo.getJavaVersion()).thenReturn(JavaVersion.JAVA_8_NEWEST);
        when(mojo.getContainerSettings()).thenReturn(containerSetting);
        MojoExecutionException exception = null;
        try {
            facade.getRuntimeHandler();
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }

        // <javaVersion> == null && Public Docker Container Image
        when(mojo.getJavaVersion()).thenReturn(null);
        when(mojo.getContainerSettings()).thenReturn(containerSetting);

        handler = facade.getRuntimeHandler();
        assertTrue(handler instanceof PublicDockerHubRuntimeHandlerImpl);

        // <javaVersion> == null && Private Docker Container Image
        when(mojo.getJavaVersion()).thenReturn(null);
        when(mojo.getContainerSettings()).thenReturn(containerSetting);
        containerSetting.setServerId("serverId");

        handler = facade.getRuntimeHandler();
        assertTrue(handler instanceof PrivateDockerHubRuntimeHandlerImpl);

        // <javaVersion> == null && Private Registry Image
        when(mojo.getJavaVersion()).thenReturn(null);
        when(mojo.getContainerSettings()).thenReturn(containerSetting);
        containerSetting.setServerId("serverId");
        containerSetting.setRegistryUrl(new URL("https://microsoft.azurecr.io"));

        handler = facade.getRuntimeHandler();
        assertTrue(handler instanceof PrivateRegistryRuntimeHandlerImpl);
    }

    @Test
    public void getSettingsHandler() throws Exception {
        final SettingsHandler handler = facade.getSettingsHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof SettingsHandlerImpl);
    }

    private List<Resource> getResourceList() {
        final Resource resource = new Resource();
        resource.setDirectory("/");
        resource.setTargetPath("/");

        final List<Resource> resources = new ArrayList<>();
        resources.add(resource);

        return resources;
    }
}
