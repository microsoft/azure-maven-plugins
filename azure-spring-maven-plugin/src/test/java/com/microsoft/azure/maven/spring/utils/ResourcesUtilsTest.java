/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import org.apache.maven.model.Resource;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResourcesUtilsTest {

    @Test
    public void testGetDefaultResources() {
        final List<Resource> resources = ResourcesUtils.getDefaultResources();
        assertEquals(1, resources.size());
        final Resource resource = resources.get(0);
        assertNotNull(resource);
        assertEquals("${project.basedir}/target", resource.getDirectory());
        assertNull(resource.getFiltering());
        assertEquals(0, resource.getExcludes().size());
        assertEquals(1, resource.getIncludes().size());
        assertEquals("*.jar", resource.getIncludes().get(0));
    }

    @Test
    public void testApplyDefaultResourcesToDom4j() throws Exception {
        final SAXReader reader = new SAXReader();
        final Document document = reader.read(this.getClass().getClassLoader().getResourceAsStream("test-2.xml"));
        final Element rootNode = document.getRootElement();
        ResourcesUtils.applyDefaultResourcesToDom4j(rootNode);
        assertTrue(rootNode.asXML().contains(
                "<resources><resource><filtering/><mergeId/><targetPath/><directory>${project.basedir}/target</directory>" +
                "<includes><include>*.jar</include></includes></resource></resources>"));
    }
}
