/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import com.microsoft.azure.maven.spring.utils.ResourcesUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeploymentTest {
    @Test
    public void testWithCpu() {
        final Deployment deploy = new Deployment();
        deploy.withCpu(1);
        assertTrue(1 == deploy.getCpu());
    }

    @Test
    public void testWithMemoryInGB() {
        final Deployment deploy = new Deployment();
        deploy.withMemoryInGB(2);
        assertTrue(2 == deploy.getMemoryInGB());
    }

    @Test
    public void testWithInstanceCount() {
        final Deployment deploy = new Deployment();
        deploy.withInstanceCount(3);
        assertTrue(3 == deploy.getInstanceCount());
    }

    @Test
    public void testWithDeploymentName() {
        final Deployment deploy = new Deployment();
        deploy.withDeploymentName("deploymentName1");
        assertEquals("deploymentName1", deploy.getDeploymentName());
    }

    @Test
    public void testWithJvmOptions() {
        final Deployment deploy = new Deployment();
        deploy.withJvmOptions("jvmOptions1");
        assertEquals("jvmOptions1", deploy.getJvmOptions());
    }

    @Test
    public void testWithEnvironment() {
        final Deployment deploy = new Deployment();
        deploy.withEnvironment(Collections.singletonMap("foo", "bar"));
        assertEquals("bar", deploy.getEnvironment().get("foo"));
    }

    @Test
    public void testWithVolumes() {
        final Deployment deploy = new Deployment();
        final Volume volume = new Volume();
        volume.withPersist(false);
        volume.withPath("/home/shared");
        volume.withSize("10G");
        final List<Volume> volumes = Arrays.asList(volume);
        deploy.withVolumes(volumes);
        assertEquals(volumes.get(0), deploy.getTemporaryDisk());
        assertNull(deploy.getPersistentDisk());

        volume.withPersist(true);
        assertEquals(volumes.get(0), deploy.getPersistentDisk());
        assertNull(deploy.getTemporaryDisk());
    }

    @Test
    public void testWithResources() {
        final Deployment deploy = new Deployment();

        deploy.withResources(ResourcesUtils.getDefaultResources());
        assertEquals(1, deploy.getResources().size());
        assertEquals("*.jar", deploy.getResources().get(0).getIncludes().get(0));
    }

    @Test
    public void testNullVolumes() {
        final Deployment deploy = new Deployment();
        assertNull(deploy.getPersistentDisk());
        assertNull(deploy.getTemporaryDisk());
    }
}
