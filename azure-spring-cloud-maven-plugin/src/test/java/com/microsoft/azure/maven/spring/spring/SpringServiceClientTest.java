/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({SpringServiceClient.class})
public class SpringServiceClientTest {

    private SpringServiceClient spyClient;

    @Before
    public void setUp() {
        final SpringServiceClient springServiceClient = new SpringServiceClient(new AzureTokenCredentials(null, null) {
            @Override
            public String getToken(String s) throws IOException {
                return null;
            }
        }, "subscriptionId", "userAgent");
        spyClient = spy(springServiceClient);
    }

    @Test
    public void getClusterByName() {
        final ServiceResourceInner mockCluster = mock(ServiceResourceInner.class);
        doReturn("existCluster").when(mockCluster).name();
        final List<ServiceResourceInner> mockClusterList = new ArrayList<>();
        mockClusterList.add(mockCluster);

        doReturn(mockClusterList).when(spyClient).getAvailableClusters();
        assertSame(spyClient.getClusterByName("existCluster"), mockCluster);

        try {
            spyClient.getClusterByName("unExistCluster");
            fail("Should throw IPE when cluster doesn't exist");
        } catch (InvalidParameterException e) {
            // Should throw IPE when cluster doesn't exist
        }
    }

    @Test
    public void getResourceGroupByCluster() {
        final ServiceResourceInner mockCluster = mock(ServiceResourceInner.class);
        doReturn("/resourceGroups/test").when(mockCluster).id();

        doReturn(mockCluster).when(spyClient).getClusterByName(any());
        assertEquals("test", spyClient.getResourceGroupByCluster("cluster"));
    }
}
