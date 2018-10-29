/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.appservice.WebAppBase.UpdateStages.WithWebContainer;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
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
    private AbstractWebAppMojo mojo;

    private final WindowsRuntimeHandlerImpl.Builder builder = new WindowsRuntimeHandlerImpl.Builder();

    private WindowsRuntimeHandlerImpl handler = null;

    @Before
    public void setup() {
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
            .javaVersion(runtime.getJavaVersion())
            .webContainer(runtime.getWebContainer())
            .build();
    }

    private void initHandlerV1() throws AzureAuthFailureException {
        handler = builder.appName(mojo.getAppName())
            .resourceGroup(mojo.getResourceGroup())
            .region(mojo.getRegion())
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup((mojo.getAppServicePlanResourceGroup()))
            .azure(mojo.getAzureClient())
            .log(mojo.getLog())
            .javaVersion(mojo.getJavaVersion())
            .webContainer(mojo.getJavaWebContainer())
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
        doReturn(WebContainer.TOMCAT_8_5_NEWEST).when(mojo).getJavaWebContainer();

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

        final RuntimeSetting runtime = mock(RuntimeSetting.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn(WebContainer.TOMCAT_8_5_NEWEST).when(runtime).getWebContainer();
        doReturn(JavaVersion.JAVA_8_NEWEST).when(runtime).getJavaVersion();

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
