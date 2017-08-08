/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.telemetry.TelemetryProxy;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAzureMojoTest {
    public static final String PLUGIN_NAME = "maven-plugin-lib";
    public static final String PLUGIN_VERSION = "0.1.0-SNAPSHOT";
    public static final String SUBSCRIPTION_ID = "fake-subscription-id";

    @Mock
    MavenProject project;

    @Mock
    MavenSession session;

    @Mock
    File buildDirectory;

    @Mock
    Settings settings;

    @Mock
    MavenResourcesFiltering filtering;

    @Mock
    PluginDescriptor plugin;

    @Mock
    AuthenticationSetting authenticationSetting;

    @Mock
    Azure azure;

    @Mock
    TelemetryProxy telemetryProxy;

    @InjectMocks
    private AbstractAzureMojo mojo = new AbstractAzureMojo() {
        @Override
        protected void doExecute() throws Exception {
        }
    };

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(plugin.getArtifactId()).thenReturn(PLUGIN_NAME);
        when(plugin.getVersion()).thenReturn(PLUGIN_VERSION);
        when(buildDirectory.getAbsolutePath()).thenReturn("target");
        ReflectionUtils.setVariableValueInObject(mojo, "subscriptionId", SUBSCRIPTION_ID);
        ReflectionUtils.setVariableValueInObject(mojo, "allowTelemetry", false);
        ReflectionUtils.setVariableValueInObject(mojo, "failsOnError", true);
    }

    @Test
    public void getProject() throws Exception {
        assertEquals(project, mojo.getProject());
    }

    @Test
    public void getSession() throws Exception {
        assertEquals(session, mojo.getSession());
    }

    @Test
    public void getMavenResourcesFiltering() throws Exception {
        assertEquals(filtering, mojo.getMavenResourcesFiltering());
    }

    @Test
    public void getBuildDirectoryAbsolutePath() throws Exception {
        assertEquals("target", mojo.getBuildDirectoryAbsolutePath());
    }

    @Test
    public void getAuthenticationSetting() throws Exception {
        assertEquals(authenticationSetting, mojo.getAuthenticationSetting());
    }

    @Test
    public void getAzureClient() throws Exception {
        assertEquals(azure, mojo.getAzureClient());
    }

    @Test
    public void getMavenSettings() {
        assertEquals(settings, mojo.getSettings());
    }

    @Test
    public void getSubscriptionId() throws Exception {
        assertEquals(SUBSCRIPTION_ID, mojo.getSubscriptionId());
    }

    @Test
    public void getTelemetryProxy() {
        assertEquals(telemetryProxy, mojo.getTelemetryProxy());
    }

    @Test
    public void isTelemetryAllowed() throws Exception {
        assertTrue(!mojo.isTelemetryAllowed());
    }

    @Test
    public void getUserAgent() {
        final String userAgent = mojo.getUserAgent();
        assertTrue(StringUtils.contains(userAgent, PLUGIN_NAME));
        assertTrue(StringUtils.contains(userAgent, PLUGIN_VERSION));
        assertTrue(StringUtils.contains(userAgent, mojo.getInstallationId()));
        assertTrue(StringUtils.contains(userAgent, mojo.getSessionId()));
    }

    @Test
    public void execute() throws Exception {
        when(azure.subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        mojo.execute();
    }

    @Test
    public void processException() throws Exception {
        final String message = "test exception message";
        String actualMessage = null;
        try {
            mojo.handleException(new Exception(message));
        } catch (Exception e) {
            actualMessage = e.getMessage();
        }
        assertEquals(message, actualMessage);
    }
}
