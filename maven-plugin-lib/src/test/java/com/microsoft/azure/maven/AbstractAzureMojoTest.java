/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.telemetry.TelemetryProxy;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAzureMojoTest {
    public static final String PLUGIN_NAME = "maven-plugin-lib";
    public static final String PLUGIN_VERSION = "0.1.0-SNAPSHOT";

    @Mock
    Settings settings;

    @Mock
    PluginDescriptor plugin;

    @Mock
    Azure azure;

    @Mock
    TelemetryProxy telemetryProxy;

    @InjectMocks
    private AbstractAzureMojo mojo = new AbstractAzureMojo() {
        public boolean isFailingOnError() {
            return true;
        }

        @Override
        protected void doExecute() throws Exception {
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(plugin.getArtifactId()).thenReturn(PLUGIN_NAME);
        when(plugin.getVersion()).thenReturn(PLUGIN_VERSION);
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
    public void getTelemetryProxy() {
        assertEquals(telemetryProxy, mojo.getTelemetryProxy());
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
        when(azure.subscriptionId()).thenReturn("fake-subscription-id");
        mojo.execute();
    }

    @Test
    public void processException() throws Exception {
        final String message = "test exception message";
        String actualMessage = null;
        try {
            mojo.processException(new Exception(message));
        } catch (Exception e) {
            actualMessage = e.getMessage();
        }
        assertEquals(message, actualMessage);
    }
}
