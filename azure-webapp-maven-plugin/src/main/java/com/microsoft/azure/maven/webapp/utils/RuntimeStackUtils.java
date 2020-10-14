/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.management.appservice.RuntimeStack;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.microsoft.azure.maven.webapp.utils.JavaVersionUtils.JAVA;
import static com.microsoft.azure.maven.webapp.utils.JavaVersionUtils.JAVA_11_STRING;
import static com.microsoft.azure.maven.webapp.utils.JavaVersionUtils.JAVA_SE;
import static com.microsoft.azure.maven.webapp.utils.JavaVersionUtils.equalsJavaVersion;
import static com.microsoft.azure.maven.webapp.utils.JavaVersionUtils.formatJavaVersion;

public class RuntimeStackUtils {

    private static final List<String> JAVA_STACKS = Arrays.asList(RuntimeStack.JAVA_8_JRE8.stack(),
            RuntimeStack.TOMCAT_8_5_JRE8.stack(), RuntimeStack.JBOSS_EAP_7_2_JAVA8.stack());
    private static final List<RuntimeStack> RUNTIME_STACKS = new ArrayList<>();

    static {
        // Init runtimeStack list
        for (final Field field : FieldUtils.getAllFieldsList(RuntimeStack.class)) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(RuntimeStack.class)) {
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
    }

    public static String getJavaVersionFromRuntimeStack(RuntimeStack runtimeStack) {
        final String javaVersion = runtimeStack.version().split("-")[1];
        return JavaVersionUtils.formatJavaVersion(javaVersion);
    }

    public static String getWebContainerFromRuntimeStack(RuntimeStack runtimeStack) {
        final String stack = StringUtils.capitalize(StringUtils.lowerCase(runtimeStack.stack()));
        final String version = runtimeStack.version();
        return stack.equalsIgnoreCase(JAVA) ?
                JAVA_SE : stack + " " + version.split("-")[0];
    }

    public static RuntimeStack getJavaSERuntimeStack(String javaVersion) {
        for (final RuntimeStack runtimeStack : getValidRuntimeStacks()) {
            if (runtimeStack.stack().equals(JAVA) &&
                    equalsJavaVersion(getJavaVersionFromRuntimeStack(runtimeStack), javaVersion)) {
                return runtimeStack;
            }
        }
        return null;
    }

    public static RuntimeStack getRuntimeStack(String javaVersion, String webContainer) {
        if (StringUtils.isEmpty(webContainer) || JavaVersionUtils.getValidJavaVersions().contains(webContainer) ||
                (Objects.nonNull(JavaVersionUtils.parseJavaVersionEnum(webContainer)) ||
                    Objects.nonNull(Utils.findStringInCollectionIgnoreCase(Arrays.asList(JAVA, JAVA_SE, JAVA_11_STRING),
                        webContainer)))) {
            return getJavaSERuntimeStack(javaVersion);
        }
        final String formattedJavaVersion = formatJavaVersion(javaVersion);
        for (final RuntimeStack runtimeStack : getValidRuntimeStacks()) {
            if (getJavaVersionFromRuntimeStack(runtimeStack).equalsIgnoreCase(formattedJavaVersion) &&
                    getWebContainerFromRuntimeStack(runtimeStack).equalsIgnoreCase(webContainer)) {
                return runtimeStack;
            }
        }
        return null;
    }

    public static List<RuntimeStack> getValidRuntimeStacks() {
        return new ArrayList<>(RUNTIME_STACKS);
    }

    public static List<String> getValidLinuxRuntimeStacksForJavaVersion(String javaVersion) {
        final Set<String> result = new HashSet<>();
        for (final RuntimeStack runtimeStack : getValidRuntimeStacks()) {
            if (equalsJavaVersion(getJavaVersionFromRuntimeStack(runtimeStack), javaVersion) &&
                    !runtimeStack.stack().equals(JAVA)) {
                result.add(getWebContainerFromRuntimeStack(runtimeStack));
            }
        }
        return new ArrayList<>(result);
    }
}
