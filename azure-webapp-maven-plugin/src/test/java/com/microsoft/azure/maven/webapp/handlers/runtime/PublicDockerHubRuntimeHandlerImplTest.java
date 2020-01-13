/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;

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
    private WebAppConfiguration config;

    @Mock
    private Azure azureClient;

    private PublicDockerHubRuntimeHandlerImpl.Builder builder = new PublicDockerHubRuntimeHandlerImpl.Builder();

    private PublicDockerHubRuntimeHandlerImpl handler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initHandlerV2() {
        handler = builder.appName(config.getAppName())
            .resourceGroup(config.getResourceGroup())
            .region(config.getRegion())
            .pricingTier(config.getPricingTier())
            .servicePlanName(config.getServicePlanName())
            .servicePlanResourceGroup((config.getServicePlanResourceGroup()))
            .azure(azureClient)
            .image(config.getImage())
            .registryUrl(config.getRegistryUrl())
            .build();
    }

    private void initHandlerV1() {
        handler = builder.appName(config.getAppName())
            .resourceGroup(config.getResourceGroup())
            .region(config.getRegion())
            .pricingTier(config.getPricingTier())
            .servicePlanName(config.getServicePlanName())
            .servicePlanResourceGroup((config.getServicePlanResourceGroup()))
            .azure(azureClient)
            .image(config.getImage())
            .registryUrl(config.getRegistryUrl())
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
        doReturn("nginx").when(config).getImage();

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
        doReturn("nginx").when(config).getImage();

        initHandlerV1();
        handler.updateAppRuntime(app);

        verify(update, times(1)).withPublicDockerHubImage(any(String.class));
        verifyNoMoreInteractions(update);
    }
}
