/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common;

import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Locale;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class
 */
public final class Utils {
    private static final String POM = "pom";
    private static final String JAR = "jar";
    private static final String WAR = "war";
    private static final String EAR = "ear";
    private static final String SUBSCRIPTIONS = "subscriptions";

    public static String getArtifactCompileVersion(File artifact) throws AzureExecutionException {
        try (JarFile jarFile = new JarFile(artifact)) {
            final JarEntry jarEntry = jarFile.stream().filter(entry -> StringUtils.endsWith(entry.getName(), ".class"))
                    .findFirst()
                    .orElseThrow(() -> new AzureExecutionException("Failed to parse artifact compile version, no class file founded in target artifact"));
            // Read compile version from class file
            // Refers https://en.wikipedia.org/wiki/Java_class_file#General_layout
            final InputStream stream = jarFile.getInputStream(jarEntry);
            final byte[] version = new byte[2];
            stream.skip(6);
            stream.read(version);
            stream.close();
            final int majorVersion = new BigInteger(version).intValueExact() - 44;
            return majorVersion > 8 ? String.valueOf(majorVersion) : String.format("1.%d", majorVersion);
        } catch (IOException e) {
            throw new AzureExecutionException("Failed to parse artifact compile version.", e);
        }
    }

    public static OperatingSystemEnum parseOperationSystem(final String os) throws AzureExecutionException {
        if (StringUtils.isEmpty(os)) {
            throw new AzureExecutionException("The value of 'os' is empty, please specify it in 'runtime' configuration.");
        }
        switch (os.toLowerCase(Locale.ENGLISH)) {
            case "windows":
                return OperatingSystemEnum.Windows;
            case "linux":
                return OperatingSystemEnum.Linux;
            case "docker":
                return OperatingSystemEnum.Docker;
            default:
                throw new AzureExecutionException("The value of <os> is unknown, supported values are: windows, " +
                        "linux and docker.");
        }
    }

    public static boolean isGUID(String input) {
        try {
            return UUID.fromString(input).toString().equalsIgnoreCase(input);
        } catch (Exception e) {
            return false;
        }
    }

    // Copied from https://github.com/microsoft/azure-tools-for-java/blob/azure-intellij-toolkit-v3.39.0/Utils/
    // azuretools-core/src/com/microsoft/azuretools/core/mvp/model/AzureMvpModel.java
    // Todo: Remove duplicated utils function in azure-tools-for-java
    public static String getSegment(String id, String segment) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        final String[] attributes = StringUtils.lowerCase(id).split("/");
        final int pos = ArrayUtils.indexOf(attributes, StringUtils.lowerCase(segment));
        if (pos >= 0) {
            return attributes[pos + 1];
        }
        return null;
    }

    public static String getSubscriptionId(String resourceId) {
        return getSegment(resourceId, SUBSCRIPTIONS);
    }

    public static boolean isPomPackagingProject(String packaging) {
        return POM.equalsIgnoreCase(packaging);
    }

    public static boolean isJarPackagingProject(String packaging) {
        return JAR.equalsIgnoreCase(packaging);
    }

    public static boolean isWarPackagingProject(String packaging) {
        return WAR.equalsIgnoreCase(packaging);
    }

    public static boolean isEarPackagingProject(String packaging) {
        return EAR.equalsIgnoreCase(packaging);
    }
}
