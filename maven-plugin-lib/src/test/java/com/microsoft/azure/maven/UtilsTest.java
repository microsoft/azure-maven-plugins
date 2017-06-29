/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {
    @Mock
    private Settings settings;

    @Mock
    private Server server;

    @Mock
    private Xpp3Dom configuration;

    @Mock
    private Xpp3Dom node;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsStringEmpty() {
        assertTrue(Utils.isStringEmpty(null));
        assertTrue(Utils.isStringEmpty(""));
        assertTrue(Utils.isStringEmpty("   "));
        assertFalse(Utils.isStringEmpty("string"));
    }

    @Test
    public void testGetServer() {
        final String invalidServerId = "non-existing";
        final String validServerId = "existing";

        when(settings.getServer(invalidServerId)).thenReturn(null);
        when(settings.getServer(validServerId)).thenReturn(new Server());

        assertNull(Utils.getServer(null, validServerId));
        assertNull(Utils.getServer(settings, null));
        assertNull(Utils.getServer(settings, invalidServerId));
        assertNotNull(Utils.getServer(settings, validServerId));
    }

    @Test
    public void testGetValueFromServerConfiguration() {
        assertNull(null, Utils.getValueFromServerConfiguration(null, null));

        when(server.getConfiguration()).thenReturn(null);
        assertNull(null, Utils.getValueFromServerConfiguration(server, "random-key"));
        verify(server, times(1)).getConfiguration();
        verifyNoMoreInteractions(server);

        when(server.getConfiguration()).thenReturn(configuration);
        assertNull(null, Utils.getValueFromServerConfiguration(server, "random-key"));
        verify(configuration, times(1)).getChild(any(String.class));
        verifyNoMoreInteractions(configuration);

        when(server.getConfiguration()).thenReturn(configuration);
        when(configuration.getChild(any(String.class))).thenReturn(node);
        when(node.getValue()).thenReturn("randomValue");
        assertEquals("randomValue", Utils.getValueFromServerConfiguration(server, "random-key"));
        verify(node, times(1)).getValue();
        verifyNoMoreInteractions(node);
    }
}
