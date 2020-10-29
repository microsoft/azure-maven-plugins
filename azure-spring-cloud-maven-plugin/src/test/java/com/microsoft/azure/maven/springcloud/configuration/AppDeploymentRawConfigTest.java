/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud.configuration;

import com.microsoft.azure.maven.springcloud.config.AppDeploymentRawConfig;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class AppDeploymentRawConfigTest {
    @Test
    public void testWithCpu() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.withCpu(1);
        assertEquals(1, (int) deploy.getCpu());
    }

    @Test
    public void testWithMemoryInGB() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.withMemoryInGB(2);
        assertEquals(2, (int) deploy.getMemoryInGB());
    }

    @Test
    public void testWithInstanceCount() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.withInstanceCount(3);
        assertEquals(3, (int) deploy.getInstanceCount());
    }

    @Test
    public void testWithDeploymentName() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.withDeploymentName("deploymentName1");
        assertEquals("deploymentName1", deploy.getDeploymentName());
    }

    @Test
    public void testWithJvmOptions() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.withJvmOptions("jvmOptions1");
        assertEquals("jvmOptions1", deploy.getJvmOptions());
    }

    @Test
    public void testWithEnvironment() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.withEnvironment(Collections.singletonMap("foo", "bar"));
        assertEquals("bar", deploy.getEnvironment().get("foo"));
    }

    @Test
    public void testWithResources() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();

        deploy.withResources(MavenConfigUtils.getDefaultResources());
        assertEquals(1, deploy.getResources().size());
        assertEquals("*.jar", deploy.getResources().get(0).getIncludes().get(0));
    }
}
