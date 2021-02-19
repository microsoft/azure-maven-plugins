/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.configuration;

import com.microsoft.azure.maven.springcloud.config.AppDeploymentRawConfig;
import com.microsoft.azure.maven.springcloud.config.ConfigurationUpdater;
import com.microsoft.azure.maven.utils.PomUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpringCloudDeploymentRawConfigTest {
    @Test
    public void testSetCpu() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.setCpu("1");
        assertEquals("1", deploy.getCpu());
    }

    @Test
    public void testSetMemoryInGB() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.setMemoryInGB("2");
        assertEquals("2", deploy.getMemoryInGB());
    }

    @Test
    public void testSetInstanceCount() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.setInstanceCount("3");
        assertEquals("3", deploy.getInstanceCount());
    }

    @Test
    public void testSetDeploymentName() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.setDeploymentName("deploymentName1");
        assertEquals("deploymentName1", deploy.getDeploymentName());
    }

    @Test
    public void testSetJvmOptions() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.setJvmOptions("jvmOptions1");
        assertEquals("jvmOptions1", deploy.getJvmOptions());
    }

    @Test
    public void testSetRuntimeVersion() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.setRuntimeVersion("8");
        assertEquals("8", deploy.getRuntimeVersion());
    }

    @Test
    public void testSaveToXml() {
        final AppDeploymentRawConfig deploy = new AppDeploymentRawConfig();
        deploy.setCpu("1");
        deploy.setMemoryInGB("2");
        deploy.setInstanceCount("3");
        deploy.setRuntimeVersion("8");
        deploy.setJvmOptions("jvmOptions1");
        final Document document = DocumentHelper.createDocument();
        final Element root = document.addElement("test");
        PomUtils.updateNode(root, ConfigurationUpdater.toMap(deploy));
        final String xml = "<test>" +
            "<cpu>1</cpu>" +
            "<memoryInGB>2</memoryInGB>" +
            "<instanceCount>3</instanceCount>" +
            "<jvmOptions>jvmOptions1</jvmOptions>" +
            "<runtimeVersion>8</runtimeVersion>" +
            "</test>" +
            "";
        assertEquals(xml, root.asXML());
    }
}
