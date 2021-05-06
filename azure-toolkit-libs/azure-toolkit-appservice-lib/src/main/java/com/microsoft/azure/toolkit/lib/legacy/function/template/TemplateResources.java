/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Correspond to resources.json, translate attribute name to its real value
public class TemplateResources {
    private static final String VARIABLES_PREFIX = "variables_";
    private static final Pattern PATTERN = Pattern.compile("\\[variables\\('(.*)'\\)\\]");
    private static Map<String, String> map;

    // Load resources from resources.json
    static {
        try (final InputStream is = TemplateResources.class.getResourceAsStream("/resources.json")) {
            final ObjectMapper mapper = new ObjectMapper();
            final String resourceJsonStr = IOUtils.toString(is, "utf8");
            final JsonNode node = mapper.readTree(resourceJsonStr);
            map = mapper.convertValue(node.get("en"), Map.class);
        } catch (Exception e) {
        }
    }

    public static String getResourceByNameWithDollar(String name) {
        return map == null ? null : map.get(name.substring(1));
    }

    public static String getResourceByVariableName(String variableName) {
        return map.get(VARIABLES_PREFIX + variableName);
    }

    public static String getResource(String name) {
        final Matcher matcher = PATTERN.matcher(name);
        if (matcher.find()) {
            return getResourceByVariableName(matcher.group(1));
        } else {
            return getResourceByNameWithDollar(name);
        }
    }
}
