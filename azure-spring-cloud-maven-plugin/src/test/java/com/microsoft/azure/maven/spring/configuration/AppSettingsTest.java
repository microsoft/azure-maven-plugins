/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppSettingsTest {
    @Test
    public void testSetSubscriptionId() {
        final AppSettings app = new AppSettings();
        app.setSubscriptionId("subscriptionId1");
        assertEquals("subscriptionId1", app.getSubscriptionId());
    }

    @Test
    public void testSetClusterName() {
        final AppSettings app = new AppSettings();
        app.setClusterName("clusterName1");
        assertEquals("clusterName1", app.getClusterName());
    }

    @Test
    public void testSetAppName() {
        final AppSettings app = new AppSettings();
        app.setAppName("appName1");
        assertEquals("appName1", app.getAppName());
    }

    @Test
    public void testSetIsPublic() {
        final AppSettings app = new AppSettings();
        app.setPublic("true");
        assertEquals("true", app.isPublic());
    }

    @Test
    public void testSaveToDom4j() {
        final AppSettings app = new AppSettings();
        app.setSubscriptionId("subscriptionId1");
        app.setClusterName("clusterName1");
        app.setAppName("appName1");
        app.setPublic("true");
        final Document document = DocumentHelper.createDocument();
        final Element root = document.addElement("test");
        app.applyToDom4j(root);
        final String xml = "<test>" +
                "<subscriptionId>subscriptionId1</subscriptionId>" +
                "<clusterName>clusterName1</clusterName>" +
                "<appName>appName1</appName>" +
                "<isPublic>true</isPublic></test>";
        assertEquals(xml, root.asXML());

        app.setPublic(null);
    }

    @Test
    public void testSaveToDom4jNullProperty() {
        final AppSettings app = new AppSettings();
        app.setSubscriptionId("subscriptionId1");
        app.setClusterName("clusterName1");
        app.setAppName("appName1");
        final Document document = DocumentHelper.createDocument();
        final Element root = document.addElement("test");
        app.applyToDom4j(root);
        final String xml = "<test>" +
                "<subscriptionId>subscriptionId1</subscriptionId>" +
                "<clusterName>clusterName1</clusterName>" +
                "<appName>appName1</appName>" +
                "</test>";
        assertEquals(xml, root.asXML());
    }
}
