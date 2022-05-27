/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.configuration;

import com.microsoft.azure.maven.springcloud.config.AppDeploymentMavenConfig;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class AppDeploymentMavenConfigTest {
    @Test
    public void testWithCpu() {
        final AppDeploymentMavenConfig deploy = new AppDeploymentMavenConfig();
        deploy.setCpu(1.0);
        assertEquals(1.0, (Object) deploy.getCpu());
    }

    @Test
    public void testWithMemoryInGB() {
        final AppDeploymentMavenConfig deploy = new AppDeploymentMavenConfig();
        deploy.setMemoryInGB(2.0);
        assertEquals(2.0, (Object) deploy.getMemoryInGB());
    }

    @Test
    public void testWithInstanceCount() {
        final AppDeploymentMavenConfig deploy = new AppDeploymentMavenConfig();
        deploy.setInstanceCount(3);
        assertEquals(3, (int) deploy.getInstanceCount());
    }

    @Test
    public void testWithDeploymentName() {
        final AppDeploymentMavenConfig deploy = new AppDeploymentMavenConfig();
        deploy.setDeploymentName("deploymentName1");
        assertEquals("deploymentName1", deploy.getDeploymentName());
    }

    @Test
    public void testWithJvmOptions() {
        final AppDeploymentMavenConfig deploy = new AppDeploymentMavenConfig();
        deploy.setJvmOptions("jvmOptions1");
        assertEquals("jvmOptions1", deploy.getJvmOptions());
    }

    @Test
    public void testWithEnvironment() {
        final AppDeploymentMavenConfig deploy = new AppDeploymentMavenConfig();
        deploy.setEnvironment(Collections.singletonMap("foo", "bar"));
        assertEquals("bar", deploy.getEnvironment().get("foo"));
    }

    @Test
    public void testWithResources() {
        final AppDeploymentMavenConfig deploy = new AppDeploymentMavenConfig();

        deploy.setResources(MavenConfigUtils.getDefaultResources());
        assertEquals(1, deploy.getResources().size());
        assertEquals("*.jar", deploy.getResources().get(0).getIncludes().get(0));
    }
}
