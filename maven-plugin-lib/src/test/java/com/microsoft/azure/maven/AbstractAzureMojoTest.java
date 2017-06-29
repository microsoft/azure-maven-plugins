/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.management.Azure;
import org.apache.maven.settings.Settings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAzureMojoTest {
    @Mock
    Settings settings;

    @Mock
    Azure azure;

    @InjectMocks
    private AbstractAzureMojo mojo = new AbstractAzureMojo() {
        public String getPluginName() {
            return "maven-plugin-lib";
        }
        public String getPluginVersion() {
            return "0.1.0-SNAPSHOT";
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAzureClient() throws Exception {
        assertEquals(azure, mojo.getAzureClient());
    }

    @Test
    public void testGetMavenSettings() {
        assertEquals(settings, mojo.getSettings());
    }

    @Test
    public void testInitTelemetry() {
        assertNotNull(mojo.getTelemetryProxy());
    }
}
