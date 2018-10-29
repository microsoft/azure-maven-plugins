/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class PublicDockerHubRuntimeHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private PublicDockerHubRuntimeHandlerImpl.Builder builder = new PublicDockerHubRuntimeHandlerImpl.Builder();

    private PublicDockerHubRuntimeHandlerImpl handler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initHandlerV2() throws AzureAuthFailureException {
        final RuntimeSetting runtime = mojo.getRuntime();
        handler = builder.appName(mojo.getAppName())
            .resourceGroup(mojo.getResourceGroup())
            .region(mojo.getRegion())
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup((mojo.getAppServicePlanResourceGroup()))
            .azure(mojo.getAzureClient())
            .log(mojo.getLog())
            .image(runtime.getImage())
            .serverId(runtime.getServerId())
            .registryUrl(runtime.getRegistryUrl())
            .build();
    }

    private void initHandlerV1() throws AzureAuthFailureException {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        handler = builder.appName(mojo.getAppName())
            .resourceGroup(mojo.getResourceGroup())
            .region(mojo.getRegion())
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup((mojo.getAppServicePlanResourceGroup()))
            .azure(mojo.getAzureClient())
            .log(mojo.getLog())
            .image(containerSetting.getImageName())
            .serverId(containerSetting.getServerId())
            .registryUrl(containerSetting.getRegistryUrl())
            .build();
    }

    @Test
    public void updateAppRuntimeV2() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        final WebApp.Update update = mock(WebApp.Update.class);
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();
        doReturn(update).when(app).update();

        final RuntimeSetting runtime = mock(RuntimeSetting.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn("nginx").when(runtime).getImage();

        initHandlerV2();
        handler.updateAppRuntime(app);

        verify(update, times(1)).withPublicDockerHubImage("nginx");
        verifyNoMoreInteractions(update);
    }

    @Test
    public void updateAppRuntimeV1() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        final Update update = mock(Update.class);
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();
        doReturn(update).when(app).update();
        final ContainerSetting containerSetting = new ContainerSetting();
        containerSetting.setImageName("nginx");
        doReturn(containerSetting).when(mojo).getContainerSettings();

        initHandlerV1();
        handler.updateAppRuntime(app);

        verify(update, times(1)).withPublicDockerHubImage(any(String.class));
        verifyNoMoreInteractions(update);
    }
}
