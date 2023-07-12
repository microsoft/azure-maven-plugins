/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.configuration;

import com.microsoft.azure.maven.springcloud.config.AppRawConfig;
import com.microsoft.azure.maven.springcloud.config.ClusterRawConfig;
import com.microsoft.azure.maven.springcloud.config.ConfigurationUpdater;
import com.microsoft.azure.maven.utils.PomUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpringCloudAppSettingsTest {
    @Test
    public void testSetAppName() {
        final AppRawConfig app = new AppRawConfig();
        app.setAppName("appName1");
        assertEquals("appName1", app.getAppName());
    }

    @Test
    public void testSetIsPublic() {
        final AppRawConfig app = new AppRawConfig();
        app.setIsPublic("true");
        assertEquals("true", app.getIsPublic());
    }

    @Test
    public void testSaveToDom4j() {
        final AppRawConfig app = new AppRawConfig();
        final ClusterRawConfig cluster = new ClusterRawConfig();
        cluster.setSubscriptionId("subscriptionId1");
        cluster.setClusterName("clusterName1");
        app.setCluster(cluster);
        app.setAppName("appName1");
        app.setIsPublic("true");
        final Document document = DocumentHelper.createDocument();
        final Element root = document.addElement("test");
        PomUtils.updateNode(root, ConfigurationUpdater.toMap(app));
        final String xml = "<test>" +
            "<subscriptionId>subscriptionId1</subscriptionId>" +
            "<clusterName>clusterName1</clusterName>" +
            "<appName>appName1</appName>" +
            "<isPublic>true</isPublic></test>";
        assertEquals(xml, root.asXML());

        app.setIsPublic(null);
    }

    @Test
    public void testSaveToDom4jNullProperty() {
        final AppRawConfig app = new AppRawConfig();
        final ClusterRawConfig cluster = new ClusterRawConfig();
        cluster.setSubscriptionId("subscriptionId1");
        cluster.setClusterName("clusterName1");
        app.setCluster(cluster);
        app.setAppName("appName1");
        final Document document = DocumentHelper.createDocument();
        final Element root = document.addElement("test");
        PomUtils.updateNode(root, ConfigurationUpdater.toMap(app));
        final String xml = "<test>" +
            "<subscriptionId>subscriptionId1</subscriptionId>" +
            "<clusterName>clusterName1</clusterName>" +
            "<appName>appName1</appName>" +
            "</test>";
        assertEquals(xml, root.asXML());
    }
}
