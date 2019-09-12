/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import com.microsoft.azure.maven.spring.utils.ResourcesUtils;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class DeploymentTest {
    @Test
    public void testWithCpu() {
        final Deployment deploy = new Deployment();
        deploy.withCpu(1);
        assertEquals(1, (int) deploy.getCpu());
    }

    @Test
    public void testWithMemoryInGB() {
        final Deployment deploy = new Deployment();
        deploy.withMemoryInGB(2);
        assertEquals(2, (int) deploy.getMemoryInGB());
    }

    @Test
    public void testWithInstanceCount() {
        final Deployment deploy = new Deployment();
        deploy.withInstanceCount(3);
        assertEquals(3, (int) deploy.getInstanceCount());
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
    public void testWithResources() {
        final Deployment deploy = new Deployment();

        deploy.withResources(ResourcesUtils.getDefaultResources());
        assertEquals(1, deploy.getResources().size());
        assertEquals("*.jar", deploy.getResources().get(0).getIncludes().get(0));
    }
}
