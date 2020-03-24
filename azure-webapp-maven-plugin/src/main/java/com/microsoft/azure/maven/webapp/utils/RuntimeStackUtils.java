/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.management.appservice.RuntimeStack;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuntimeStackUtils {

    private static final List<String> JAVA_STACKS = Arrays.asList("JAVA", "TOMCAT");
    private static final List<RuntimeStack> RUNTIME_STACKS = new ArrayList<>();
    private static final BidiMap<String, String> JAVA_VERSIONS = new DualHashBidiMap<>();

    static {
        // Init runtimeStack list
        for (final Field field : RuntimeStack.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    final RuntimeStack runtimeStack = (RuntimeStack) field.get(null);
                    if (JAVA_STACKS.contains(runtimeStack.stack())) {
                        RUNTIME_STACKS.add(runtimeStack);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        // init map for java version and its displayname
        JAVA_VERSIONS.put("Java 8", "jre8");
        JAVA_VERSIONS.put("Java 11", "java11");
    }

    public static String getJavaVersionFromRuntimeStack(RuntimeStack runtimeStack) {
        return runtimeStack.version().split("-")[1];
    }

    public static String getWebContainerFromRuntimeStack(RuntimeStack runtimeStack) {
        final String stack = runtimeStack.stack();
        final String version = runtimeStack.version();
        return stack.equalsIgnoreCase("JAVA") ?
                version.split("-")[1] : stack + " " + version.split("-")[0];
    }

    public static RuntimeStack getRuntimeStack(String javaVersion) {
        for (final RuntimeStack runtimeStack : getValidRuntimeStacks()) {
            if (runtimeStack.stack().equals("JAVA") &&
                    getJavaVersionFromRuntimeStack(runtimeStack).equalsIgnoreCase(javaVersion)) {
                return runtimeStack;
            }
        }
        return null;
    }

    public static RuntimeStack getRuntimeStack(String javaVersion, String webContainer) {
        if (StringUtils.isEmpty(webContainer) || getValidJavaVersions().containsValue(webContainer)) {
            return getRuntimeStack(javaVersion);
        }
        for (final RuntimeStack runtimeStack : getValidRuntimeStacks()) {
            if (getJavaVersionFromRuntimeStack(runtimeStack).equalsIgnoreCase(javaVersion) &&
                    getWebContainerFromRuntimeStack(runtimeStack).equalsIgnoreCase(webContainer)) {
                return runtimeStack;
            }
        }
        return null;
    }

    public static List<RuntimeStack> getValidRuntimeStacks() {
        return new ArrayList<>(RUNTIME_STACKS);
    }

    public static List<String> getValidWebContainer(String javaVersion) {
        final Set<String> result = new HashSet<>();
        for (final RuntimeStack runtimeStack : getValidRuntimeStacks()) {
            if (getJavaVersionFromRuntimeStack(runtimeStack).equalsIgnoreCase(javaVersion) &&
                    !runtimeStack.stack().equals("JAVA")) {
                result.add(getWebContainerFromRuntimeStack(runtimeStack));
            }
        }
        return new ArrayList<>(result);
    }

    public static BidiMap<String, String> getValidJavaVersions() {
        return JAVA_VERSIONS;
    }

}
