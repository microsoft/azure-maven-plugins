/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public class WebContainer {
    private String value;

    public static final WebContainer TOMCAT_7_0_NEWEST = new WebContainer("tomcat 7.0");
    public static final WebContainer TOMCAT_7_0_50 = new WebContainer("tomcat 7.0.50");
    public static final WebContainer TOMCAT_7_0_62 = new WebContainer("tomcat 7.0.62");
    public static final WebContainer TOMCAT_8_0_NEWEST = new WebContainer("tomcat 8.0");
    public static final WebContainer TOMCAT_8_0_23 = new WebContainer("tomcat 8.0.23");
    public static final WebContainer TOMCAT_8_5_NEWEST = new WebContainer("tomcat 8.5");
    public static final WebContainer TOMCAT_8_5_6 = new WebContainer("tomcat 8.5.6");
    public static final WebContainer TOMCAT_8_5_20 = new WebContainer("tomcat 8.5.20");
    public static final WebContainer TOMCAT_8_5_31 = new WebContainer("tomcat 8.5.31");
    public static final WebContainer TOMCAT_8_5_34 = new WebContainer("tomcat 8.5.34");
    public static final WebContainer TOMCAT_8_5_37 = new WebContainer("tomcat 8.5.37");
    public static final WebContainer TOMCAT_9_0_NEWEST = new WebContainer("tomcat 9.0");
    public static final WebContainer TOMCAT_9_0_0 = new WebContainer("tomcat 9.0.0");
    public static final WebContainer TOMCAT_9_0_8 = new WebContainer("tomcat 9.0.8");
    public static final WebContainer TOMCAT_9_0_12 = new WebContainer("tomcat 9.0.12");
    public static final WebContainer TOMCAT_9_0_14 = new WebContainer("tomcat 9.0.14");
    public static final WebContainer JETTY_9_1_NEWEST = new WebContainer("jetty 9.1");
    public static final WebContainer JETTY_9_1_V20131115 = new WebContainer("jetty 9.1.0.20131115");
    public static final WebContainer JETTY_9_3_NEWEST = new WebContainer("jetty 9.3");
    public static final WebContainer JETTY_9_3_V20161014 = new WebContainer("jetty 9.3.13.20161014");
    public static final WebContainer JAVA_8 = new WebContainer("java 8");

    public static WebContainer getWebContainer(String container, String containerVersion) {
        final String containerValue = String.format("%s %s", container, containerVersion);
        return values().stream()
                .filter(webContainer -> StringUtils.equals(webContainer.value, container))
                .findFirst().orElse(null);
    }

    public static List<WebContainer> values() {
        return Arrays.asList(TOMCAT_7_0_NEWEST, TOMCAT_7_0_50, TOMCAT_7_0_62, TOMCAT_8_0_NEWEST, TOMCAT_8_0_23, TOMCAT_8_5_NEWEST,
                TOMCAT_8_5_6, TOMCAT_8_5_20, TOMCAT_8_5_31, TOMCAT_8_5_34, TOMCAT_8_5_37, TOMCAT_9_0_NEWEST, TOMCAT_9_0_0, TOMCAT_9_0_8,
                TOMCAT_9_0_12, TOMCAT_9_0_14, JETTY_9_1_NEWEST, JETTY_9_1_V20131115, JETTY_9_3_NEWEST, JETTY_9_3_V20161014, JAVA_8);
    }

    public static WebContainer createFromServiceModel(com.azure.resourcemanager.appservice.models.WebContainer webContainer) {
        return values().stream()
                .filter(value -> StringUtils.equals(value.value, webContainer.toString()))
                .findFirst().orElse(null);
    }

    public static com.azure.resourcemanager.appservice.models.WebContainer convertToServiceModel(WebContainer webContainer) {
        return com.azure.resourcemanager.appservice.models.WebContainer.values().stream()
                .filter(value -> StringUtils.equals(value.toString(), webContainer.getValue()))
                .findFirst().orElse(null);
    }
}
