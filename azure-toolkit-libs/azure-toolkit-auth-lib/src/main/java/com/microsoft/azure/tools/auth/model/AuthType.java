/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.model;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

/**
 * The auth type which user may define in configuration.
 */
public enum AuthType {
    AUTO,
    AZURE_CLI,
    MANAGED_IDENTITY,
    VSCODE,
    INTELLIJ_IDEA,
    VISUAL_STUDIO,
    DEVICE_CODE,
    OAUTH2;

    public static AuthType parseAuthType(String type) {
        if (StringUtils.isBlank(type)) {
            return AUTO;
        }
        switch (type.toLowerCase().trim()) {
            case "azure_cli":
                return AZURE_CLI;
            case "intellij":
                return INTELLIJ_IDEA;
            case "vscode":
                return VSCODE;
            case "device_code":
                return DEVICE_CODE;
            case "managed_identity":
                return MANAGED_IDENTITY;
            case "oauth2":
                return OAUTH2;
            case "visual_studio":
                return VISUAL_STUDIO;
            case "auto":
                return AUTO;
        }
        throw new UnsupportedOperationException(String.format("Invalid auth type '%s', supported values are: %s.", type,
                StringUtils.join(Arrays.stream(values()).map(t -> StringUtils.lowerCase(t.toString())), ",")));
    }
}
