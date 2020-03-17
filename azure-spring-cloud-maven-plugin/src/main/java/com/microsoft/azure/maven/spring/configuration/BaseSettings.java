/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import com.microsoft.azure.maven.spring.utils.XmlUtils;

import org.dom4j.Element;

import java.util.Map;

public abstract class BaseSettings {
    public void applyToDom4j(Element ele) {
        for (final Map.Entry<String, Object> entry : getProperties().entrySet()) {
            if (entry.getValue() != null) {
                XmlUtils.addDomWithKeyValue(ele, entry.getKey(), entry.getValue());
            }
        }
    }

    protected abstract Map<String, Object> getProperties();
}
