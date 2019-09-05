/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.spring.utils;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XmlUtilsTest {
    private Element propertiesNode;

    @Before
    public void setup() throws Exception {
        final SAXReader reader = new SAXReader();
        final Document document = reader.read(this.getClass().getClassLoader().getResourceAsStream("test.xml"));

        final Element rootElement = document.getRootElement();
        final Namespace ns = rootElement.getNamespace();
        propertiesNode = rootElement.element(new QName("properties", ns));

    }

    @Test
    public void testPrettyPrintElementNoNamespace() throws Exception {
        final String[] lines = IndentUtil.splitLines(XmlUtils.prettyPrintElementNoNamespace(propertiesNode));
        assertEquals(5, lines.length);
        assertEquals("<properties>", lines[0]);
        assertEquals("    <maven.compiler.source>1.8</maven.compiler.source>", lines[1]);
        assertEquals("    <maven.compiler.target>1.8</maven.compiler.target>", lines[2]);
        assertEquals("    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>", lines[3]);
        assertEquals("</properties>", lines[4]);
    }

    @Test
    public void testGetChildValue() {
        assertEquals("1.8\n", XmlUtils.getChildValue("maven.compiler.target", propertiesNode));
    }

    @Test
    public void testAddDomWithKeyValue() {
        XmlUtils.addDomWithKeyValue(propertiesNode, "foo", "bar");
        final String[] lines = IndentUtil.splitLines(XmlUtils.prettyPrintElementNoNamespace(propertiesNode));
        assertEquals(6, lines.length);
        assertEquals("    <foo>bar</foo>", lines[4]);
    }

    @Test
    public void testAddDomWithKeyValueList() {
        XmlUtils.addDomWithValueList(propertiesNode, "foo", "list", Arrays.asList("bar1", "bar2"));
        final String[] lines = IndentUtil.splitLines(XmlUtils.prettyPrintElementNoNamespace(propertiesNode));
        assertEquals(9, lines.length);
        assertEquals("    <foo>", lines[4]);
        assertEquals("        <list>bar1</list>", lines[5]);
        assertEquals("        <list>bar2</list>", lines[6]);
        assertEquals("    </foo>", lines[7]);
    }

    @Test
    public void testTrimTextBeforeEnd() {
        XmlUtils.removeAllNamespaces(propertiesNode);
        XmlUtils.addDomWithKeyValue(propertiesNode, "foo", "bar");

        final Namespace ns = propertiesNode.getNamespace();
        final Element fooNode = propertiesNode.element(new QName("foo", ns));
        propertiesNode.addText("      ");
        final String xmlBefore = propertiesNode.asXML();
        XmlUtils.trimTextBeforeEnd(propertiesNode, fooNode);
        final String xmlAfter = propertiesNode.asXML();
        assertTrue(xmlBefore.contains("</project.build.sourceEncoding>\n  <foo>bar</foo>      </properties>"));
        assertTrue(xmlAfter.contains("</project.build.sourceEncoding><foo>bar</foo></properties>"));

    }
}
