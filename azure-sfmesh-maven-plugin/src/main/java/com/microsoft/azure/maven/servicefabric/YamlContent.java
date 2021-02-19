/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.servicefabric;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class YamlContent {
    public static class Builder{
        final HashMap<String, String> properties = new HashMap<String, String>();

        public Builder addElement(String key, String value){
            properties.put(key, value);
            return this;
        }

        public String build(Log logger, String resourceName) throws MojoFailureException {
            final StringBuilder replacedYamlContent = new StringBuilder();
            try {
                final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resourceName);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                while (reader.ready()){
                    final String line = reader.readLine().replace("\n", "");
                    final String wordToReplace = line.substring(line.lastIndexOf(" ") + 1);
                    if (properties.containsKey(wordToReplace)){
                        logger.debug(String.format("Replacing %s with %s", wordToReplace,
                            properties.get(wordToReplace)));
                        replacedYamlContent.append(line.replace(wordToReplace, properties.get(wordToReplace)));
                    } else {
                        replacedYamlContent.append(line);
                    }
                    replacedYamlContent.append("\n");
                }
                reader.close();
                inputStream.close();
            } catch (IOException e){
                logger.error(e);
                throw new MojoFailureException(String.format("Error while building " +
                    "%s resource", resourceName));
            }
            return replacedYamlContent.toString();
        }
    }

    private YamlContent(){
    }
}
