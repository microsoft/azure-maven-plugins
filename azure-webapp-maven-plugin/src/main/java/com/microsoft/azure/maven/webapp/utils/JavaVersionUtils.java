/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaVersionUtils {
    static final String JAVA_11_STRING = "java 11";
    static final String JAVA_SE = "Java SE";
    static final String JAVA = "JAVA";
    private static final List<String> JAVA_VERSIONS = new ArrayList<>();

    static {
        JAVA_VERSIONS.add(JavaVersion.JAVA_8.toString());
        JAVA_VERSIONS.add(JavaVersion.JAVA_11.toString());
    }

    public static JavaVersion toAzureSdkJavaVersion(String javaVersion) {
        if (StringUtils.isEmpty(javaVersion)) {
            return null;
        }
        final JavaVersion newJavaVersion = JavaVersion.fromString(javaVersion);
        if (newJavaVersion == JavaVersion.OFF) {
            throw new AzureToolkitRuntimeException(String.format("Cannot parse java version: '%s'.", javaVersion));
        }
        return newJavaVersion;
    }

    public static String formatJavaVersion(JavaVersion javaVersion) {
        if (Objects.isNull(javaVersion)) {
            return null;
        }

        return Objects.toString(javaVersion, null);
    }

    public static List<String> getValidJavaVersions() {
        return JAVA_VERSIONS;
    }

    public static String formatJavaVersion(String javaVersion) {
        return Objects.toString(JavaVersion.fromString(javaVersion), null);
    }
}
