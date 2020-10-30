/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud.configuration;

import com.microsoft.azure.tools.springcloud.AppConfig;
import com.microsoft.azure.tools.springcloud.AppDeploymentConfig;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class SpringConfigurationTest {
    @Test
    public void testWithIsPublic() {
        final AppConfig config = new AppConfig();
        config.withPublic(Boolean.TRUE);
        assertEquals(Boolean.TRUE, config.isPublic());
    }

    @Test
    public void testWithSubscriptionId() {
        final AppConfig config = new AppConfig();
        config.withSubscriptionId("subscriptionId1");
        assertEquals("subscriptionId1", config.getSubscriptionId());
    }

    @Test
    public void testWithResourceGroup() {
        final AppConfig config = new AppConfig();
        config.withResourceGroup("resourceGroup1");
        assertEquals("resourceGroup1", config.getResourceGroup());
    }

    @Test
    public void testWithClusterName() {
        final AppConfig config = new AppConfig();
        config.withClusterName("clusterName1");
        assertEquals("clusterName1", config.getClusterName());
    }

    @Test
    public void testWithAppName() {
        final AppConfig config = new AppConfig();
        config.withAppName("appName1");
        assertEquals("appName1", config.getAppName());
    }

    @Test
    public void testWithRuntimeVersion() {
        final AppConfig config = new AppConfig();
        config.withRuntimeVersion("runtimeVersion1");
        assertEquals("runtimeVersion1", config.getRuntimeVersion());
    }

    @Test
    public void testWithDeployment() {
        final AppConfig config = new AppConfig();
        final AppDeploymentConfig deploy = Mockito.mock(AppDeploymentConfig.class);
        config.withDeployment(deploy);
        assertEquals(deploy, config.getDeployment());
    }
}
