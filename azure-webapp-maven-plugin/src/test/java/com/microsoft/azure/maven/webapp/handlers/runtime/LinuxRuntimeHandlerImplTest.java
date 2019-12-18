/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LinuxRuntimeHandlerImplTest {
    @Mock
    private WebAppConfiguration config;

    @Mock
    private Azure azureClient;

    @Mock
    private Log log;

    private final LinuxRuntimeHandlerImpl.Builder builder = new LinuxRuntimeHandlerImpl.Builder();

    private LinuxRuntimeHandlerImpl handler = null;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void initHandlerV2() {
        handler = builder.runtime(config.getRuntimeStack())
            .appName(config.getAppName())
            .resourceGroup(config.getResourceGroup())
            .region(config.getRegion())
            .pricingTier(config.getPricingTier())
            .servicePlanName(config.getServicePlanName())
            .servicePlanResourceGroup((config.getServicePlanResourceGroup()))
            .azure(azureClient)
            .build();
    }

    private void initHandlerV1() {
        handler = builder.runtime(config.getRuntimeStack())
            .appName(config.getAppName())
            .resourceGroup(config.getResourceGroup())
            .region(config.getRegion())
            .pricingTier(config.getPricingTier())
            .servicePlanName(config.getServicePlanName())
            .servicePlanResourceGroup((config.getServicePlanResourceGroup()))
            .azure(azureClient)
            .build();
    }

    @Test
    public void updateAppRuntimeTestV2() throws Exception {
        final WebApp app = mock(WebApp.class);
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn(siteInner).when(app).inner();
        doReturn("app,linux").when(siteInner).kind();
        final WebApp.Update update = mock(WebApp.Update.class);
        doReturn(update).when(app).update();
        doReturn(update).when(update).withBuiltInImage(RuntimeStack.TOMCAT_8_5_JRE8);
        doReturn(RuntimeStack.TOMCAT_8_5_JRE8).when(config).getRuntimeStack();

        initHandlerV2();

        assertSame(update, handler.updateAppRuntime(app));
        verify(update, times(1)).withBuiltInImage(RuntimeStack.TOMCAT_8_5_JRE8);
    }

    @Test
    public void updateAppRuntimeTestV1() throws Exception {
        final WebApp app = mock(WebApp.class);
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        doReturn(siteInner).when(app).inner();
        final Update update = mock(Update.class);
        doReturn(update).when(app).update();
        doReturn(update).when(update).withBuiltInImage(any(RuntimeStack.class));
        doReturn(RuntimeStack.TOMCAT_8_5_JRE8).when(config).getRuntimeStack();

        initHandlerV1();
        assertSame(update, handler.updateAppRuntime(app));
        verify(update, times(1)).withBuiltInImage(any(RuntimeStack.class));
    }
}
