/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.utils;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.function.configurations.FunctionExtensionVersion;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;


public class FunctionUtils {

    private static final String INVALID_FUNCTION_EXTENSION_VERSION = "FUNCTIONS_EXTENSION_VERSION is empty or invalid, " +
            "please check the configuration";

    public static FunctionExtensionVersion parseFunctionExtensionVersion(String version) throws AzureExecutionException {
        return Arrays.stream(FunctionExtensionVersion.values())
                .filter(versionEnum -> StringUtils.equalsIgnoreCase(versionEnum.getVersion(), version))
                .findFirst()
                .orElseThrow(() -> new AzureExecutionException(INVALID_FUNCTION_EXTENSION_VERSION));
    }
}
