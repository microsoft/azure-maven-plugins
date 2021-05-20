/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum FunctionDeployType {
    FTP,
    ZIP,
    MSDEPLOY,
    RUN_FROM_ZIP,
    RUN_FROM_BLOB;

    private static final String UNKNOWN_DEPLOYMENT_TYPE = "The value of <deploymentType> is unknown.";

    public static FunctionDeployType fromString(final String input) {
        return Arrays.stream(FunctionDeployType.values())
                .filter(type -> StringUtils.equalsAnyIgnoreCase(type.name(), input))
                .findFirst().orElseThrow(() -> new AzureToolkitRuntimeException(UNKNOWN_DEPLOYMENT_TYPE));
    }
}
