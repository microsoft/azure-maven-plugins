/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import com.google.common.collect.Sets;
import com.microsoft.azure.toolkit.lib.common.model.ExpandableParameter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JavaVersion implements ExpandableParameter {
    private static final String JAVA_7_VALUE = "7";
    private static final String JAVA_7_DISPLAY_NAME = "Java 7";
    private static final String JAVA_8_VALUE = "8";
    private static final String JAVA_8_DISPLAY_NAME = "Java 8";
    private static final String JAVA_11_DISPLAY_NAME = "Java 11";

    public static final JavaVersion OFF = new JavaVersion("<null>");
    public static final JavaVersion JAVA_7 = new JavaVersion("1.7");
    public static final JavaVersion JAVA_8 = new JavaVersion("1.8");
    public static final JavaVersion JAVA_11 = new JavaVersion("11");

    private static final JavaVersion JAVA_1_7_0_51 = new JavaVersion("1.7.0_51");
    private static final JavaVersion JAVA_1_7_0_71 = new JavaVersion("1.7.0_71");
    private static final JavaVersion JAVA_1_7_0_80 = new JavaVersion("1.7.0_80");
    private static final JavaVersion JAVA_ZULU_1_7_0_191 = new JavaVersion("1.7.0_191_ZULU");
    private static final JavaVersion JAVA_1_8_0_25 = new JavaVersion("1.8.0_25");
    private static final JavaVersion JAVA_1_8_0_60 = new JavaVersion("1.8.0_60");
    private static final JavaVersion JAVA_1_8_0_73 = new JavaVersion("1.8.0_73");
    private static final JavaVersion JAVA_1_8_0_111 = new JavaVersion("1.8.0_111");
    private static final JavaVersion JAVA_1_8_0_144 = new JavaVersion("1.8.0_144");
    private static final JavaVersion JAVA_1_8_0_172 = new JavaVersion("1.8.0_172");
    private static final JavaVersion JAVA_ZULU_1_8_0_172 = new JavaVersion("1.8.0_172_ZULU");
    private static final JavaVersion JAVA_ZULU_1_8_0_92 = new JavaVersion("1.8.0_92");
    private static final JavaVersion JAVA_ZULU_1_8_0_102 = new JavaVersion("1.8.0_102");
    private static final JavaVersion JAVA_1_8_0_181 = new JavaVersion("1.8.0_181");
    private static final JavaVersion JAVA_ZULU_1_8_0_181 = new JavaVersion("1.8.0_181_ZULU");
    private static final JavaVersion JAVA_1_8_0_202 = new JavaVersion("1.8.0_202");
    private static final JavaVersion JAVA_ZULU_1_8_0_202 = new JavaVersion("1.8.0_202_ZULU");
    private static final JavaVersion JAVA_ZULU_11_0_2 = new JavaVersion("11.0.2_ZULU");

    private static final Set<JavaVersion> values = Collections.unmodifiableSet(Sets.newHashSet(OFF, JAVA_7, JAVA_1_7_0_51, JAVA_1_7_0_71, JAVA_1_7_0_80,
            JAVA_ZULU_1_7_0_191, JAVA_8, JAVA_1_8_0_25, JAVA_1_8_0_60, JAVA_1_8_0_73, JAVA_1_8_0_111, JAVA_1_8_0_144, JAVA_1_8_0_172, JAVA_ZULU_1_8_0_172,
            JAVA_ZULU_1_8_0_92, JAVA_ZULU_1_8_0_102, JAVA_1_8_0_181, JAVA_ZULU_1_8_0_181, JAVA_1_8_0_202, JAVA_ZULU_1_8_0_202, JAVA_11, JAVA_ZULU_11_0_2));

    private String value;

    public static Set<JavaVersion> values() {
        return values;
    }

    public static JavaVersion fromString(String input) {
        if (StringUtils.isEmpty(input)) {
            return JavaVersion.OFF;
        }
        // remove java, jre prefix
        final String version = StringUtils.lowerCase(input).replaceFirst("java|jre", "").trim();
        // handle java 7/ java 8 cases
        if (StringUtils.equalsIgnoreCase(version, JAVA_7_VALUE)) {
            return JavaVersion.JAVA_7;
        }
        if (StringUtils.equalsIgnoreCase(version, JAVA_8_VALUE)) {
            return JavaVersion.JAVA_8;
        }
        return values().stream()
                .filter(javaVersion -> StringUtils.equalsIgnoreCase(version, javaVersion.getValue()))
                .findFirst().orElse(new JavaVersion(input));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JavaVersion)) {
            return false;
        }
        // users may get javaVersion by setValue, which will skip the parse step in fromString, so compare the parsed value in equals
        // todo: update the implement once we find solution to serialize parameters without public setter/constructor
        final JavaVersion current = JavaVersion.fromString(value);
        final JavaVersion toCompare = JavaVersion.fromString(((JavaVersion) o).value);
        return StringUtils.equals(current.value, toCompare.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        if (this.equals(JAVA_7)) {
            return JAVA_7_DISPLAY_NAME;
        }
        if (this.equals(JAVA_8)) {
            return JAVA_8_DISPLAY_NAME;
        }
        if (this.equals(JAVA_11)) {
            return JAVA_11_DISPLAY_NAME;
        }
        return this.value;
    }

    @Override
    public boolean isExpandedValue() {
        return !values().contains(this);
    }
}
