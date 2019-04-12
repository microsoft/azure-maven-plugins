/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.management.appservice.RuntimeStack;
import org.codehaus.plexus.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuntimeStackUtils {

    private static final List<String> JAVA_STACKS = Arrays.asList("JAVA", "TOMCAT", "WILDFLY");
    private static final List<RuntimeStack> runtimeStacks = new ArrayList<>();

    static {
        // Init runtimeStack list
        for (final Field field : RuntimeStack.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    final RuntimeStack runtimeStack = (RuntimeStack) field.get(null);
                    if (JAVA_STACKS.contains(runtimeStack.stack())) {
                        runtimeStacks.add(runtimeStack);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getJavaVersionFromRuntimeStack(RuntimeStack runtimeStack) {
        return runtimeStack.version().split("-")[1];
    }

    public static String getWebContainerFromRuntimeStack(RuntimeStack runtimeStack) {
        return String.format(runtimeStack.stack() + " " + runtimeStack.version().split("-")[0]);
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
        if (StringUtils.isEmpty(webContainer) || getValidJavaVersions().contains(webContainer)) {
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
        return new ArrayList<>(runtimeStacks);
    }

    public static List<String> getValidWebContainer(String javaVersion) {
        final Set<String> result = new HashSet<>();
        for (final RuntimeStack runtimeStack : getValidRuntimeStacks()) {
            if (getJavaVersionFromRuntimeStack(runtimeStack).equalsIgnoreCase(javaVersion) &&
                !runtimeStack.stack().equals("JAVA")) {
                result.add(getWebContainerFromRuntimeStack(runtimeStack));
            }
        }
        result.add(javaVersion);
        return new ArrayList<>(result);
    }

    public static List<String> getValidJavaVersions() {
        final Set<String> result = new HashSet<>();
        for (final RuntimeStack runtimeStack : getValidRuntimeStacks()) {
            result.add(getJavaVersionFromRuntimeStack(runtimeStack));
        }
        return new ArrayList<>(result);
    }
}
