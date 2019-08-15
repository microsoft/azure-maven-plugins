/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppClusterResourceInner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SpringServiceUtils.class})
public class SpringServiceUtilsTest {

    @Test
    public void getClusterByName() {
        final AppClusterResourceInner mockCluster = PowerMockito.mock(AppClusterResourceInner.class);
        PowerMockito.doReturn("existCluster").when(mockCluster).name();
        final List<AppClusterResourceInner> mockClusterList = new ArrayList<>();
        mockClusterList.add(mockCluster);

        PowerMockito.stub(PowerMockito.method(SpringServiceUtils.class, "getAvailableClusters")).toReturn(mockClusterList);
        assertTrue(SpringServiceUtils.getClusterByName("existCluster") == mockCluster);

        try {
            SpringServiceUtils.getClusterByName("unExistCluster");
            fail("Should throw IPE when cluster doesn't exist");
        } catch (InvalidParameterException e) {
            // Should throw IPE when cluster doesn't exist
        }
    }

    @Test
    public void getResourceGroupByCluster() {
        final AppClusterResourceInner mockCluster = PowerMockito.mock(AppClusterResourceInner.class);
        PowerMockito.doReturn("/resourceGroups/test").when(mockCluster).id();
        PowerMockito.stub(PowerMockito.method(SpringServiceUtils.class, "getClusterByName", String.class)).toReturn(mockCluster);
        assertTrue(SpringServiceUtils.getResourceGroupByCluster("cluster").equals("test"));
    }
}
