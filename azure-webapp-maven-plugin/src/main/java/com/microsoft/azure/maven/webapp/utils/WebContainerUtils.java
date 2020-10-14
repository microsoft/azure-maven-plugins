/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.management.appservice.WebContainer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.azure.maven.webapp.utils.JavaVersionUtils.JAVA;
import static com.microsoft.azure.maven.webapp.utils.JavaVersionUtils.JAVA_11_STRING;
import static com.microsoft.azure.maven.webapp.utils.JavaVersionUtils.JAVA_SE;

public class WebContainerUtils {
    static {
        // Add 'Java SE' in WebContainer to since there is already a javaVersion setting.
        WebContainer.fromString(JAVA_SE);

        // keep 'java 11' in WebContainer is to parse existing web containers which are
        // using: java 11, they need to be changed to Java SE
        WebContainer.fromString(JAVA_11_STRING);
    }

    public static WebContainer getJavaSEWebContainer() {
        return WebContainer.fromString(JAVA_SE);
    }

    public static String formatWebContainer(WebContainer webContainer) {
        if (Objects.isNull(webContainer)) {
            return null;
        }
        if (StringUtils.equalsIgnoreCase(JAVA_11_STRING, webContainer.toString())) {
            // since java version information is set at javaVersion, for web container,
            // always return "Java SE"
            return JAVA_SE;
        }
        return StringUtils.capitalize(webContainer.toString());
    }

    public static List<String> getAvailableWebContainer() {
        final List<String> result = new ArrayList<>();
        final List<Object> fields = FieldUtils.getAllFieldsList(WebContainer.class).stream()
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getName().contains("NEWEST")).map(field -> {
                    try {
                        return FieldUtils.readStaticField(field);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }).filter(field -> field instanceof WebContainer).collect(Collectors.toList());

        for (final WebContainer webContainer : WebContainer.values()) {
            if (!StringUtils.containsIgnoreCase(webContainer.toString(), JAVA) &&
                    !StringUtils.containsIgnoreCase(webContainer.toString(), "jetty") &&
                    fields.indexOf(webContainer) >= 0) {
                result.add(webContainer.toString());
            }
        }
        Collections.sort(result);
        return result;
    }
}
