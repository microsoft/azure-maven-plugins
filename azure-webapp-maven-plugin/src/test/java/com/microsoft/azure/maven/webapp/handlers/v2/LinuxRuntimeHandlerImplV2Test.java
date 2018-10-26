/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v2;

import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LinuxRuntimeHandlerImplV2Test {
    @Mock
    private AbstractWebAppMojo mojo;

    private final LinuxRuntimeHandlerImplV2.Builder builder = new LinuxRuntimeHandlerImplV2.Builder();

    private LinuxRuntimeHandlerImplV2 handler = null;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    public void initHandler() throws AzureAuthFailureException, MojoExecutionException {
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

    @Test
    public void updateAppRuntimeTest() throws Exception {
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

        initHandler();

        assertSame(update, handler.updateAppRuntime(app));
        verify(update, times(1)).withBuiltInImage(RuntimeStack.TOMCAT_8_5_JRE8);
    }
}
