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
public class WebContainer implements ExpandableParameter {
    private static final String JAVA = "Java";
    private static final String JAVA_7 = "Java 7";
    private static final String JAVA_8 = "Java 8";
    private static final String JAVA_11 = "Java 11";
    private static final String STRING_JAVA_SE = "Java SE";

    public static final WebContainer JAVA_OFF = new WebContainer("<null>");
    public static final WebContainer JAVA_SE = new WebContainer("java se");
    public static final WebContainer TOMCAT_7 = new WebContainer("tomcat 7.0");
    public static final WebContainer TOMCAT_8 = new WebContainer("tomcat 8.0");
    public static final WebContainer TOMCAT_85 = new WebContainer("tomcat 8.5");
    public static final WebContainer TOMCAT_9 = new WebContainer("tomcat 9.0");
    public static final WebContainer JBOSS_7 = new WebContainer("JBOSSEAP 7");
    public static final WebContainer JBOSS_72 = new WebContainer("JBOSSEAP 7.2");

    public static final WebContainer TOMCAT_7_0_50 = new WebContainer("tomcat 7.0.50");
    public static final WebContainer TOMCAT_7_0_62 = new WebContainer("tomcat 7.0.62");
    public static final WebContainer TOMCAT_8_0_23 = new WebContainer("tomcat 8.0.23");
    public static final WebContainer TOMCAT_8_5_6 = new WebContainer("tomcat 8.5.6");
    public static final WebContainer TOMCAT_8_5_20 = new WebContainer("tomcat 8.5.20");
    public static final WebContainer TOMCAT_8_5_31 = new WebContainer("tomcat 8.5.31");
    public static final WebContainer TOMCAT_8_5_34 = new WebContainer("tomcat 8.5.34");
    public static final WebContainer TOMCAT_8_5_37 = new WebContainer("tomcat 8.5.37");
    public static final WebContainer TOMCAT_9_0_0 = new WebContainer("tomcat 9.0.0");
    public static final WebContainer TOMCAT_9_0_8 = new WebContainer("tomcat 9.0.8");
    public static final WebContainer TOMCAT_9_0_12 = new WebContainer("tomcat 9.0.12");
    public static final WebContainer TOMCAT_9_0_14 = new WebContainer("tomcat 9.0.14");
    public static final WebContainer JETTY_9_1_NEWEST = new WebContainer("jetty 9.1");
    public static final WebContainer JETTY_9_1_V20131115 = new WebContainer("jetty 9.1.0.20131115");
    public static final WebContainer JETTY_9_3_NEWEST = new WebContainer("jetty 9.3");
    public static final WebContainer JETTY_9_3_V20161014 = new WebContainer("jetty 9.3.13.20161014");

    private static final Set<WebContainer> values = Collections.unmodifiableSet(Sets.newHashSet(TOMCAT_7, TOMCAT_7_0_50, TOMCAT_7_0_62, TOMCAT_8,
            TOMCAT_8_0_23, TOMCAT_85, TOMCAT_8_5_6, TOMCAT_8_5_20, TOMCAT_8_5_31, TOMCAT_8_5_34, TOMCAT_8_5_37, TOMCAT_9, TOMCAT_9_0_0, TOMCAT_9_0_8,
            TOMCAT_9_0_12, TOMCAT_9_0_14, JETTY_9_1_NEWEST, JETTY_9_1_V20131115, JETTY_9_3_NEWEST, JETTY_9_3_V20161014, JAVA_SE, JBOSS_72, JBOSS_7));

    private String value;

    public static Set<WebContainer> values() {
        return values;
    }

    public static WebContainer fromString(String input) {
        // parse display name first
        if (StringUtils.isEmpty(input)) {
            return WebContainer.JAVA_OFF;
        }
        if (StringUtils.equalsAnyIgnoreCase(input, JAVA, JAVA_7, JAVA_8, JAVA_11, STRING_JAVA_SE)) {
            return WebContainer.JAVA_SE;
        }
        return values().stream()
            .filter(webContainer -> StringUtils.equalsIgnoreCase(input, webContainer.value))
            .findFirst().orElse(new WebContainer(input));
    }

    @Override
    public boolean equals(Object target) {
        if (this == target) {
            return true;
        }
        if (target == null || getClass() != target.getClass()) {
            return false;
        }
        // todo: update the implement once we find solution to serialize parameters without public setter/constructor
        final WebContainer current = WebContainer.fromString(value);
        final WebContainer toCompare = WebContainer.fromString(((WebContainer) target).value);
        return StringUtils.equals(current.value, toCompare.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        if (this.equals(JAVA_OFF)) {
            return this.value;
        }

        if (this.equals(JAVA_SE)) {
            return STRING_JAVA_SE;
        }
        return StringUtils.capitalize(StringUtils.lowerCase(value));
    }

    @Override
    public boolean isExpandedValue() {
        return !values().contains(this);
    }
}
