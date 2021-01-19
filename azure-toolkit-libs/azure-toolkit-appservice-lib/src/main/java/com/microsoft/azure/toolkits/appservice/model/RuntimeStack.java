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
public class RuntimeStack {
    private String stack;
    private String version;

    public static final RuntimeStack JAVA8 = new RuntimeStack("JAVA", "8-jre8");
    public static final RuntimeStack JAVA11 = new RuntimeStack("JAVA", "11-java11");
    public static final RuntimeStack JAVA8_JBOSS72 = new RuntimeStack("JBOSSEAP", "7.2-java8");
    public static final RuntimeStack JAVA8_TOMCAT9 = new RuntimeStack("TOMCAT", "9.0-jre8");
    public static final RuntimeStack JAVA8_TOMCAT85 = new RuntimeStack("TOMCAT", "8.5-jre8");
    public static final RuntimeStack JAVA11_TOMCAT9 = new RuntimeStack("TOMCAT", "9.0-java11");
    public static final RuntimeStack JAVA11_TOMCAT85 = new RuntimeStack("TOMCAT", "8.5-java11");

    public static List<RuntimeStack> values() {
        return Arrays.asList(JAVA8, JAVA11, JAVA8_JBOSS72, JAVA8_TOMCAT9, JAVA8_TOMCAT85, JAVA11_TOMCAT9, JAVA11_TOMCAT85);
    }

    public static RuntimeStack getFromLinuxFxVersion(String linuxFxVersion) {
        final String[] runtimeArray = linuxFxVersion.split("|");
        if (runtimeArray.length != 2) {
            return null;
        }
        final String stack = runtimeArray[0];
        final String version = runtimeArray[1];
        return getFromServiceModel(new com.azure.resourcemanager.appservice.models.RuntimeStack(stack, version));
    }

    public static RuntimeStack getFromServiceModel(com.azure.resourcemanager.appservice.models.RuntimeStack runtimeStack) {
        return values().stream()
                .filter(value -> StringUtils.equals(value.stack, runtimeStack.stack()) && StringUtils.equals(value.version, runtimeStack.version()))
                .findFirst().orElse(null);
    }

    public static com.azure.resourcemanager.appservice.models.RuntimeStack convertToServiceModel(RuntimeStack runtimeStack) {
        return com.azure.resourcemanager.appservice.models.RuntimeStack.getAll().stream()
                .filter(value -> StringUtils.equals(value.stack(), runtimeStack.getStack()) && StringUtils.equals(value.version(), runtimeStack.getVersion()))
                .findFirst().orElse(null);
    }
}
