/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.serializer;

import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.dom.DOMElement;

public class V1ConfigurationSerializer extends XMLSerializer {
    @Override
    public DOMElement convertToXML(WebAppConfiguration webAppConfiguration) throws MojoFailureException {
        return null;
    }
}
