/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
    public void getServer() {
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
    public void getValueFromServerConfiguration() {
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

    @Test
    public void copyResources() throws Exception {
        final MavenResourcesFiltering filtering = mock(MavenResourcesFiltering.class);
        final Resource resource = new Resource();
        resource.setTargetPath("/");

        Utils.copyResources(mock(MavenProject.class),
                mock(MavenSession.class),
                filtering,
                Arrays.asList(new Resource[] {resource}),
                "target");
        verify(filtering, times(1)).filterResources(any(MavenResourcesExecution.class));
        verifyNoMoreInteractions(filtering);
    }
}
