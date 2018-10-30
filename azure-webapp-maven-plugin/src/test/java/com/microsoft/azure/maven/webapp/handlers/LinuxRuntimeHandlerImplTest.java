/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static com.microsoft.azure.maven.webapp.WebAppUtils.getLinuxRunTimeStack;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LinuxRuntimeHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private final LinuxRuntimeHandlerImpl.Builder builder = new LinuxRuntimeHandlerImpl.Builder();

    private LinuxRuntimeHandlerImpl handler = null;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void initHandlerV2() throws AzureAuthFailureException, MojoExecutionException {
        final RuntimeSetting runtime = mojo.getRuntime();
        handler = builder.runtime(runtime.getLinuxRuntime())
            .appName(mojo.getAppName())
            .resourceGroup(mojo.getResourceGroup())
            .region(mojo.getRegion())
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup((mojo.getAppServicePlanResourceGroup()))
            .azure(mojo.getAzureClient())
            .log(mojo.getLog())
            .build();
    }

    private void initHandlerV1() throws AzureAuthFailureException, MojoExecutionException {
        handler = builder.runtime(getLinuxRunTimeStack(mojo.getLinuxRuntime()))
            .appName(mojo.getAppName())
            .resourceGroup(mojo.getResourceGroup())
            .region(mojo.getRegion())
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup((mojo.getAppServicePlanResourceGroup()))
            .azure(mojo.getAzureClient())
            .log(mojo.getLog())
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

        final RuntimeSetting runtime = mock(RuntimeSetting.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn(RuntimeStack.TOMCAT_8_5_JRE8).when(runtime).getLinuxRuntime();

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
        doReturn(RuntimeStack.TOMCAT_8_5_JRE8.toString()).when(mojo).getLinuxRuntime();

        initHandlerV1();
        assertSame(update, handler.updateAppRuntime(app));
        verify(update, times(1)).withBuiltInImage(any(RuntimeStack.class));
    }
}
