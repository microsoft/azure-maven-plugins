/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.appservice.WebAppBase.UpdateStages.WithWebContainer;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class WindowsRuntimeHandlerImplTest {
    @Mock
    private WebAppConfiguration config;

    @Mock
    private Azure azureClient;

    @Mock
    private Log log;
    private final WindowsRuntimeHandlerImpl.Builder builder = new WindowsRuntimeHandlerImpl.Builder();

    private WindowsRuntimeHandlerImpl handler = null;

    @Before
    public void setup() {
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
            .javaVersion(config.getJavaVersion())
            .webContainer(config.getWebContainer())
            .log(log)
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
            .javaVersion(config.getJavaVersion())
            .webContainer(config.getWebContainer())
            .log(log)
            .build();
    }

    @Test
    public void updateAppRuntimeTestV1() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app").when(siteInner).kind();
        final WithWebContainer withWebContainer = mock(WithWebContainer.class);
        final Update update = mock(Update.class);
        doReturn(withWebContainer).when(update).withJavaVersion(null);
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();
        doReturn(update).when(app).update();
        doReturn(WebContainer.TOMCAT_8_5_NEWEST).when(config).getWebContainer();

        initHandlerV1();
        assertSame(update, handler.updateAppRuntime(app));
        verify(withWebContainer, times(1)).withWebContainer(WebContainer.TOMCAT_8_5_NEWEST);
        verifyNoMoreInteractions(withWebContainer);
    }

    @Test
    public void updateAppRuntimeTestV2() throws Exception {
        final WebApp app = mock(WebApp.class);
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn(siteInner).when(app).inner();
        doReturn("app").when(siteInner).kind();
        doReturn(WebContainer.TOMCAT_8_5_NEWEST).when(config).getWebContainer();
        doReturn(JavaVersion.JAVA_8_NEWEST).when(config).getJavaVersion();

        final WebAppBase.UpdateStages.WithWebContainer withWebContainer =
            mock(WebAppBase.UpdateStages.WithWebContainer.class);
        final WebApp.Update update = mock(WebApp.Update.class);
        doReturn(withWebContainer).when(update).withJavaVersion(JavaVersion.JAVA_8_NEWEST);
        doReturn(update).when(app).update();

        initHandlerV2();

        assertSame(update, handler.updateAppRuntime(app));
        verify(withWebContainer, times(1)).withWebContainer(WebContainer.TOMCAT_8_5_NEWEST);
        verifyNoMoreInteractions(withWebContainer);
    }
}
