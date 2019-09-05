/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VolumeTest {
    @Test
    public void testWithPath() {
        final Volume volume = new Volume();
        volume.withPath("/home/shared");
        assertEquals("/home/shared", volume.getPath());
    }

    @Test
    public void testWithSize() {
        final Volume volume = new Volume();
        volume.withSize("10G");
        assertEquals("10G", volume.getSize());
    }

    @Test
    public void testWithPersist() {
        final Volume volume = new Volume();
        volume.withPersist(true);
        assertEquals(Boolean.TRUE, volume.isPersist());
    }
}
