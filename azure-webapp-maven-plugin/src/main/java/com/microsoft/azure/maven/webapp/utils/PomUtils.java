/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PomUtils {
    public static Document getModelFromPomFile(String file) throws IOException, DocumentException {
        final SAXReader reader = new SAXReader();
        final Document document = reader.read(new FileReader(file));
        return document;

    }

    public static void saveModelToPomFile(Document document, String file) throws IOException {
        final XMLWriter writer = new XMLWriter(new FileWriter(file));
        writer.write(document);
        writer.close();
    }

}
