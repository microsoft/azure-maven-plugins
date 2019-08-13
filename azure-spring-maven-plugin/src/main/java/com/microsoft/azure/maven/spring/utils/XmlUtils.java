/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class XmlUtils {
    public static void replaceDomWithKeyValue(Xpp3Dom node, String key, Object value) {
        if (value == null) {
            return;
        }
        if (node.getChild(key) == null) {
            node.addChild(createDomWithKeyValue(key, value.toString()));
        } else {
            node.getChild(key).setValue(value.toString());
        }
    }

    public static Xpp3Dom createDomWithName(String name) {
        return new Xpp3Dom(name);
    }

    public static Xpp3Dom createDomWithKeyValue(String name, String value) {
        final Xpp3Dom dom = new Xpp3Dom(name);
        dom.setValue(value);
        return dom;
    }
}
