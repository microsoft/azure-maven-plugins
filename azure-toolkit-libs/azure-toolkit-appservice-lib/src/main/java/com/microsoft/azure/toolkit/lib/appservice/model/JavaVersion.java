/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class JavaVersion {
    private static final String JAVA_7_VALUE = "Java 7";
    private static final String JAVA_7_VALUE_TRIM = "Java7";
    private static final String JAVA_8_VALUE = "Java 8";
    private static final String JAVA_8_SIMPLE_VALUE = "8";
    private static final String JAVA_8_VALUE_TRIM = "Java8";
    private static final String JAVA_11_VALUE = "Java 11";
    private static final String JAVA_11_SIMPLE_VALUE = "11";
    private static final String JAVA_11_VALUE_TRIM = "Java11";

    public static final JavaVersion OFF = new JavaVersion("<null>");
    public static final JavaVersion JAVA_7 = new JavaVersion("1.7");
    public static final JavaVersion JAVA_8 = new JavaVersion("1.8");
    public static final JavaVersion JAVA_11 = new JavaVersion("11");

    public static final JavaVersion JAVA_1_7_0_51 = new JavaVersion("1.7.0_51");
    public static final JavaVersion JAVA_1_7_0_71 = new JavaVersion("1.7.0_71");
    public static final JavaVersion JAVA_1_7_0_80 = new JavaVersion("1.7.0_80");
    public static final JavaVersion JAVA_ZULU_1_7_0_191 = new JavaVersion("1.7.0_191_ZULU");
    public static final JavaVersion JAVA_1_8_0_25 = new JavaVersion("1.8.0_25");
    public static final JavaVersion JAVA_1_8_0_60 = new JavaVersion("1.8.0_60");
    public static final JavaVersion JAVA_1_8_0_73 = new JavaVersion("1.8.0_73");
    public static final JavaVersion JAVA_1_8_0_111 = new JavaVersion("1.8.0_111");
    public static final JavaVersion JAVA_1_8_0_144 = new JavaVersion("1.8.0_144");
    public static final JavaVersion JAVA_1_8_0_172 = new JavaVersion("1.8.0_172");
    public static final JavaVersion JAVA_ZULU_1_8_0_172 = new JavaVersion("1.8.0_172_ZULU");
    public static final JavaVersion JAVA_ZULU_1_8_0_92 = new JavaVersion("1.8.0_92");
    public static final JavaVersion JAVA_ZULU_1_8_0_102 = new JavaVersion("1.8.0_102");
    public static final JavaVersion JAVA_1_8_0_181 = new JavaVersion("1.8.0_181");
    public static final JavaVersion JAVA_ZULU_1_8_0_181 = new JavaVersion("1.8.0_181_ZULU");
    public static final JavaVersion JAVA_1_8_0_202 = new JavaVersion("1.8.0_202");
    public static final JavaVersion JAVA_ZULU_1_8_0_202 = new JavaVersion("1.8.0_202_ZULU");
    public static final JavaVersion JAVA_ZULU_11_0_2 = new JavaVersion("11.0.2_ZULU");

    private static final List<JavaVersion> values = Collections.unmodifiableList(Arrays.asList(OFF, JAVA_7, JAVA_1_7_0_51, JAVA_1_7_0_71, JAVA_1_7_0_80,
            JAVA_ZULU_1_7_0_191, JAVA_8, JAVA_1_8_0_25, JAVA_1_8_0_60, JAVA_1_8_0_73, JAVA_1_8_0_111, JAVA_1_8_0_144, JAVA_1_8_0_172, JAVA_ZULU_1_8_0_172,
            JAVA_ZULU_1_8_0_92, JAVA_ZULU_1_8_0_102, JAVA_1_8_0_181, JAVA_ZULU_1_8_0_181, JAVA_1_8_0_202, JAVA_ZULU_1_8_0_202, JAVA_11, JAVA_ZULU_11_0_2));

    private final String value;

    public static List<JavaVersion> values() {
        return values;
    }

    public static JavaVersion fromString(String input) {
        if (StringUtils.isEmpty(input)) {
            return JavaVersion.OFF;
        }

        final String version = StringUtils.lowerCase(input).replaceFirst("jre", "java");

        // parse display name first
        if (StringUtils.equalsAnyIgnoreCase(version, JAVA_7_VALUE, JAVA_7_VALUE_TRIM)) {
            return JavaVersion.JAVA_7;
        }
        if (StringUtils.equalsAnyIgnoreCase(version, JAVA_8_VALUE, JAVA_8_VALUE_TRIM, JAVA_8_SIMPLE_VALUE)) {
            return JavaVersion.JAVA_8;
        }
        if (StringUtils.equalsAnyIgnoreCase(version, JAVA_11_VALUE, JAVA_11_VALUE_TRIM, JAVA_11_SIMPLE_VALUE)) {
            return JavaVersion.JAVA_11;
        }
        return values().stream()
                .filter(javaVersion -> StringUtils.equalsIgnoreCase(version, javaVersion.getValue()))
                .findFirst().orElse(JavaVersion.OFF);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JavaVersion)) {
            return false;
        }
        final JavaVersion that = (JavaVersion) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        if (this.equals(JAVA_7)) {
            return JAVA_7_VALUE;
        }
        if (this.equals(JAVA_8)) {
            return JAVA_8_VALUE;
        }
        if (this.equals(JAVA_11)) {
            return JAVA_11_VALUE;
        }
        return this.value;
    }
}
