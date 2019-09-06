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

public class DeploymentSettingsTest {
    @Test
    public void testSetCpu() {
        final DeploymentSettings deploy = new DeploymentSettings();
        deploy.setCpu("1");
        assertEquals("1", deploy.getCpu());
    }

    @Test
    public void testSetMemoryInGB() {
        final DeploymentSettings deploy = new DeploymentSettings();
        deploy.setMemoryInGB("2");
        assertEquals("2", deploy.getMemoryInGB());
    }

    @Test
    public void testSetInstanceCount() {
        final DeploymentSettings deploy = new DeploymentSettings();
        deploy.setInstanceCount("3");
        assertEquals("3", deploy.getInstanceCount());
    }

    @Test
    public void testSetDeploymentName() {
        final DeploymentSettings deploy = new DeploymentSettings();
        deploy.setDeploymentName("deploymentName1");
        assertEquals("deploymentName1", deploy.getDeploymentName());
    }

    @Test
    public void testSetJvmOptions() {
        final DeploymentSettings deploy = new DeploymentSettings();
        deploy.setJvmOptions("jvmOptions1");
        assertEquals("jvmOptions1", deploy.getJvmOptions());
    }

    @Test
    public void testSetRuntimeVersion() {
        final DeploymentSettings deploy = new DeploymentSettings();
        deploy.setRuntimeVersion("8");
        assertEquals("8", deploy.getRuntimeVersion());
    }

    @Test
    public void testSaveToXml() {
        final DeploymentSettings deploy = new DeploymentSettings();
        deploy.setCpu("1");
        deploy.setMemoryInGB("2");
        deploy.setInstanceCount("3");
        deploy.setRuntimeVersion("8");
        deploy.setJvmOptions("jvmOptions1");
        final Document document = DocumentHelper.createDocument();
        final Element root = document.addElement("test");
        deploy.applyToDom4j(root);
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
