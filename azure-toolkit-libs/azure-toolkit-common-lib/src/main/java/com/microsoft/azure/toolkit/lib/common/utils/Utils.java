/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.microsoft.azure.toolkit.lib.common.exception.CommandExecuteException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Utils {
    private static final boolean isWindows = System.getProperty("os.name").contains("Windows");

    public static String executeCommandAndGetOutput(final String cmd, File cwd)
        throws IOException, InterruptedException {
        final String[] cmds = new String[]{isWindows ? "cmd.exe" : "bash", isWindows ? "/c" : "-c", cmd};
        final Process p = Runtime.getRuntime().exec(cmds, null, cwd);
        final int exitCode = p.waitFor();
        if (exitCode != 0) {
            String errorLog = IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
            throw new CommandExecuteException(String.format("Cannot execute '%s' due to error: %s", cmd, errorLog));
        }
        return IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
    }

    public static Collection<String> intersectIgnoreCase(List<String> list1, List<String> list2) {
        if (CollectionUtils.isNotEmpty(list1) && CollectionUtils.isNotEmpty(list2)) {
            return list2.stream().filter(str -> containsIgnoreCase(list1, str)).collect(Collectors.toSet());
        }
        return Collections.emptyList();
    }

    public static boolean containsIgnoreCase(List<String> list, String str) {
        if (StringUtils.isNotBlank(str) && CollectionUtils.isNotEmpty(list)) {
            return list.stream().anyMatch(str2 -> StringUtils.equalsIgnoreCase(str, str2));
        }
        return false;
    }

    public static String getId(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }

    public static <T> T firstNonNull(T... args) {
        for (T obj : args) {
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    public static <T> T copyProperties(T to, T from) throws IllegalAccessException, InvocationTargetException {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        for (Field field : FieldUtils.getAllFields(from.getClass())) {
            FieldUtils.writeField(to, field.getName(), FieldUtils.readField(from, field.getName(), true), true);
        }
        return to;
    }

    public static void disableAzureIdentityLogs() {
        setPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.identity", "off");
        setPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.adal4j", "off");
        setPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.msal4jextensions", "off");
    }

    private static void setPropertyIfNotExist(String key, String value) {
        if (StringUtils.isBlank(System.getProperty(key))) {
            System.setProperty(key, value);
        }
    }
}
