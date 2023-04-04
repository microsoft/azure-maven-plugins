/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Map;

import static com.microsoft.azure.maven.AbstractAzureMojo.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAzureMojoTest {
    public static final String PLUGIN_NAME = "azure-maven-plugin-lib";
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

    @InjectMocks
    private AbstractAzureMojo mojo = new AbstractAzureMojo() {
        @Override
        protected void doExecute() {
        }
    };

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        doReturn(PLUGIN_NAME).when(plugin).getArtifactId();
        doReturn(PLUGIN_VERSION).when(plugin).getVersion();
        doReturn("target").when(buildDirectory).getAbsolutePath();
        ReflectionUtils.setVariableValueInObject(mojo, "subscriptionId", SUBSCRIPTION_ID);
        ReflectionUtils.setVariableValueInObject(mojo, "allowTelemetry", false);
        ReflectionUtils.setVariableValueInObject(mojo, "failsOnError", true);
        mojo.initTelemetryProxy();
    }

    @Test
    public void getProject() {
        assertEquals(project, mojo.getProject());
    }

    @Test
    public void getSession() {
        assertEquals(session, mojo.getSession());
    }

    @Test
    public void getMavenResourcesFiltering() {
        assertEquals(filtering, mojo.getMavenResourcesFiltering());
    }

    @Test
    public void getBuildDirectoryAbsolutePath() {
        assertEquals("target", mojo.getBuildDirectoryAbsolutePath());
    }

    @Test
    public void getMavenSettings() {
        assertEquals(settings, mojo.getSettings());
    }

    @Test
    public void getSubscriptionId() {
        assertEquals(SUBSCRIPTION_ID, mojo.getSubscriptionId());
    }

    @Test
    public void isTelemetryAllowed() {
        assertFalse(mojo.getAllowTelemetry());
    }

    @Test
    public void getUserAgentWhenTelemetryAllowed() throws IllegalAccessException {
        ReflectionUtils.setVariableValueInObject(mojo, "allowTelemetry", true);
        final String userAgent = mojo.getUserAgent();
        assertTrue(StringUtils.contains(userAgent, PLUGIN_NAME));
        assertTrue(StringUtils.contains(userAgent, PLUGIN_VERSION));
        assertTrue(StringUtils.contains(userAgent, mojo.getInstallationId()));
        assertTrue(StringUtils.contains(userAgent, mojo.getSessionId()));
    }

    @Test
    public void getUserAgentWhenTelemetryNotAllowed() {
        final String userAgent = mojo.getUserAgent();
        assertTrue(StringUtils.contains(userAgent, PLUGIN_NAME));
        assertTrue(StringUtils.contains(userAgent, PLUGIN_VERSION));
        assertFalse(StringUtils.contains(userAgent, mojo.getInstallationId()));
        assertFalse(StringUtils.contains(userAgent, mojo.getSessionId()));
    }

    @Test
    public void processException() {
        final String message = "test exception message";
        String actualMessage = null;
        try {
            mojo.onMojoError(new Exception(message));
        } catch (Exception e) {
            actualMessage = e.getMessage();
        }
        assertEquals(message, actualMessage);
    }

    @Test
    public void getTelemetryProperties() {
        final Map<String, String> map = mojo.getTelemetryProperties();

        assertEquals(5, map.size());
        assertTrue(map.containsKey(INSTALLATION_ID_KEY));
        assertTrue(map.containsKey(PLUGIN_NAME_KEY));
        assertTrue(map.containsKey(PLUGIN_VERSION_KEY));
        assertTrue(map.containsKey(SUBSCRIPTION_ID_KEY));
        assertTrue(map.containsKey(SESSION_ID_KEY));
    }
}
