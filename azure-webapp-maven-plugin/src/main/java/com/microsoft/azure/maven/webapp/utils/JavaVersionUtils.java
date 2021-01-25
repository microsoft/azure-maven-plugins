/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.maven.webapp.models.JavaVersionEnum;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class JavaVersionUtils {
    static final String JAVA_11_STRING = "java 11";
    static final String JAVA_SE = "Java SE";
    static final String JAVA = "JAVA";
    private static final String JAVA_1_8 = "1.8";
    private static final String JAVA_1_7 = "1.7";
    private static final String JAVA_11 = "11";

    private static final String JAVA_SHORT_VERSION_7 = "java7";
    private static final String JAVA_SHORT_VERSION_8 = "java8";
    private static final String JAVA_SHORT_VERSION_11 = "java11";
    private static final List<String> JAVA_VERSIONS = new ArrayList<>();

    static {
        JAVA_VERSIONS.add(JavaVersionEnum.JAVA_8.toString());
        JAVA_VERSIONS.add(JavaVersionEnum.JAVA_11.toString());
    }

    public static JavaVersionEnum toJavaVersionEnum(JavaVersion javaVersion) {
        if (Objects.isNull(javaVersion)) {
            return null;
        }

        final String version = javaVersion.toString();
        if (StringUtils.startsWith(version, JAVA_1_7)) {
            return JavaVersionEnum.JAVA_7;
        } else if (StringUtils.startsWith(version, JAVA_1_8)) {
            return JavaVersionEnum.JAVA_8;
        } else if (StringUtils.startsWith(version, JAVA_11)) {
            return JavaVersionEnum.JAVA_11;
        }
        return null;
    }

    public static JavaVersion toAzureSdkJavaVersion(String javaVersion) {
        if (StringUtils.isEmpty(javaVersion)) {
            return null;
        }
        final JavaVersionEnum newEnum = parseJavaVersionEnum(javaVersion);
        if (newEnum == null) {
            throw new IllegalArgumentException(String.format("Cannot parse java version: '%s'.", javaVersion));
        }
        switch (newEnum) {
            case JAVA_7:
                return JavaVersion.JAVA_7_NEWEST;
            case JAVA_8:
                return JavaVersion.JAVA_8_NEWEST;
            case JAVA_11:
                return JavaVersion.JAVA_11;
            default:
                throw new IllegalArgumentException(String.format("Java version '%s' is not supported.", javaVersion));
        }
    }

    public static com.microsoft.azure.toolkits.appservice.model.JavaVersion toLibraryJavaVersion(String input) {
        if (StringUtils.isEmpty(input)) {
            return null;
        }
        com.microsoft.azure.toolkits.appservice.model.JavaVersion javaVersion =
                com.microsoft.azure.toolkits.appservice.model.JavaVersion.fromString(input);
        if (javaVersion == null) {
            final JavaVersionEnum javaVersionEnum = JavaVersionUtils.parseJavaVersionEnum(input);
            switch (javaVersionEnum) {
                case JAVA_7:
                    return com.microsoft.azure.toolkits.appservice.model.JavaVersion.JAVA_7;
                case JAVA_8:
                    return com.microsoft.azure.toolkits.appservice.model.JavaVersion.JAVA_8;
                case JAVA_11:
                    return com.microsoft.azure.toolkits.appservice.model.JavaVersion.JAVA_11;
                default:
                    throw new IllegalArgumentException(String.format("Java version '%s' is not supported.", input));
            }
        }
        return javaVersion;
    }

    public static String formatJavaVersion(JavaVersion javaVersion) {
        if (Objects.isNull(javaVersion)) {
            return null;
        }

        return Objects.toString(toJavaVersionEnum(javaVersion));
    }

    public static boolean equalsJavaVersion(String version1, String version2) {
        return parseJavaVersionEnum(version1) == parseJavaVersionEnum(version2);
    }

    private static String getShortJavaVersion(String javaVersion) {
        if (Objects.isNull(javaVersion)) {
            return null;
        }
        String version = StringUtils.lowerCase(javaVersion);

        if (version.contains("jre")) {
            version = version.replaceFirst("jre", "java");
        }
        return version.replaceAll("\\s+", "");
    }

    public static JavaVersionEnum parseJavaVersionEnum(String javaVersion) {
        if (StringUtils.isEmpty(javaVersion)) {
            return null;
        }
        // 1. first check whether it is a JavaVersion, this is to keep compatibility with old settings
        final String javaVersionInEum =
                Utils.findStringInCollectionIgnoreCase(JavaVersion.values().stream().map(Object::toString).collect(Collectors.toList()), javaVersion);
        if (Objects.nonNull(javaVersionInEum)) {
            return toJavaVersionEnum(JavaVersion.fromString(javaVersionInEum));
        }
        switch (getShortJavaVersion(javaVersion)) {
            case "1.7":
            case JAVA_SHORT_VERSION_7:
                return JavaVersionEnum.JAVA_7;
            case "1.8":
            case "8":
            case JAVA_SHORT_VERSION_8:
                return JavaVersionEnum.JAVA_8;
            case "11":
            case JAVA_SHORT_VERSION_11:
                return JavaVersionEnum.JAVA_11;
            default:
                return null;
        }
    }

    public static List<String> getValidJavaVersions() {
        return JAVA_VERSIONS;
    }

    public static String formatJavaVersion(String javaVersion) {
        return Objects.toString(parseJavaVersionEnum(javaVersion));
    }
}
