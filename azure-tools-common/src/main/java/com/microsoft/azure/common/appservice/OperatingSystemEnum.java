/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.appservice;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public enum OperatingSystemEnum {
    Linux,
    Windows,
    Docker;

    public static OperatingSystemEnum fromString(final String os) throws AzureExecutionException {
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
}
