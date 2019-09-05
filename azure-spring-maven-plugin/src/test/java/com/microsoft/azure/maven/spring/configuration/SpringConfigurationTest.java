/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class SpringConfigurationTest {
    @Test
    public void testWithIsPublic() {
        final SpringConfiguration config = new SpringConfiguration();
        config.withPublic(Boolean.TRUE);
        assertEquals(Boolean.TRUE, config.isPublic());
    }

    @Test
    public void testWithSubscriptionId() {
        final SpringConfiguration config = new SpringConfiguration();
        config.withSubscriptionId("subscriptionId1");
        assertEquals("subscriptionId1", config.getSubscriptionId());
    }

    @Test
    public void testWithResourceGroup() {
        final SpringConfiguration config = new SpringConfiguration();
        config.withResourceGroup("resourceGroup1");
        assertEquals("resourceGroup1", config.getResourceGroup());
    }

    @Test
    public void testWithClusterName() {
        final SpringConfiguration config = new SpringConfiguration();
        config.withClusterName("clusterName1");
        assertEquals("clusterName1", config.getClusterName());
    }

    @Test
    public void testWithAppName() {
        final SpringConfiguration config = new SpringConfiguration();
        config.withAppName("appName1");
        assertEquals("appName1", config.getAppName());
    }

    @Test
    public void testWithRuntimeVersion() {
        final SpringConfiguration config = new SpringConfiguration();
        config.withRuntimeVersion("runtimeVersion1");
        assertEquals("runtimeVersion1", config.getRuntimeVersion());
    }

    @Test
    public void testWithDeployment() {
        final SpringConfiguration config = new SpringConfiguration();
        final Deployment deploy = Mockito.mock(Deployment.class);
        config.withDeployment(deploy);
        assertEquals(deploy, config.getDeployment());
    }
}
