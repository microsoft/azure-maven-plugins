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

import java.util.Locale;
import java.util.UUID;

/**
 * Utility class
 */
public final class Utils {

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
        final String[] attributes = id.split("/");
        int pos = ArrayUtils.indexOf(attributes, segment);
        if (pos >= 0) {
            return attributes[pos + 1];
        }
        return null;
    }
}
