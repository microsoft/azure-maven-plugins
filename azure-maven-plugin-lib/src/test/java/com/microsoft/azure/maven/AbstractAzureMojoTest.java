/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.auth.AuthenticationSetting;
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
import java.util.Map;

import static com.microsoft.azure.maven.AbstractAzureMojo.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

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
        doReturn(PLUGIN_NAME).when(plugin).getArtifactId();
        doReturn(PLUGIN_VERSION).when(plugin).getVersion();
        doReturn("target").when(buildDirectory).getAbsolutePath();
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
    public void execute() throws Exception {
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

    @Test
    public void getTelemetryProperties() throws Exception {
        final Map map = mojo.getTelemetryProperties();

        assertEquals(6, map.size());
        assertTrue(map.containsKey(INSTALLATION_ID_KEY));
        assertTrue(map.containsKey(PLUGIN_NAME_KEY));
        assertTrue(map.containsKey(PLUGIN_VERSION_KEY));
        assertTrue(map.containsKey(SUBSCRIPTION_ID_KEY));
        assertTrue(map.containsKey(SESSION_ID_KEY));
        assertTrue(map.containsKey(AUTH_TYPE));
    }

    @Test
    public void getAuthType() throws Exception {
        assertEquals("Unknown", mojo.getAuthType());

        doReturn("serverId").when(authenticationSetting).getServerId();
        doReturn(null).when(authenticationSetting).getFile();
        assertEquals("ServerId", mojo.getAuthType());

        doReturn(null).when(authenticationSetting).getServerId();
        doReturn(new File("/pom.xml")).when(authenticationSetting).getFile();
        assertEquals("AuthFile", mojo.getAuthType());

        ReflectionUtils.setVariableValueInObject(mojo, "authentication", null);
        assertEquals("AzureCLI", mojo.getAuthType());
    }

    @Test
    public void logging() throws Exception {
        mojo.debug("debug message");
        mojo.info("info message");
        mojo.warning("warning message");
        mojo.error("error message");
    }
}
