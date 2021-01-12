/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.model;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import org.apache.commons.lang3.StringUtils;

/**
 * The auth type which user may define in configuration.
 */
public enum AuthType {
    AUTO,
    SERVICE_PRINCIPAL,
    AZURE_AUTH_MAVEN_PLUGIN,
    MANAGED_IDENTITY,
    AZURE_CLI,
    VSCODE,
    INTELLIJ_IDEA,
    VISUAL_STUDIO,
    DEVICE_CODE,
    OAUTH2;

    public static AuthType parseAuthType(String type) throws InvalidConfigurationException {
        if (StringUtils.isBlank(type)) {
            return AUTO;
        }
        switch (type.toLowerCase().trim()) {
            case "auto":
                return AUTO;
            case "service_principal":
                return SERVICE_PRINCIPAL;
            case "managed_identity":
                return MANAGED_IDENTITY;
            case "azure_cli":
                return AZURE_CLI;
            case "vscode":
                return VSCODE;
            case "intellij":
                return INTELLIJ_IDEA;
            case "azure_auth_maven_plugin":
                return AZURE_AUTH_MAVEN_PLUGIN;
            case "device_code":
                return DEVICE_CODE;
            case "oauth2":
                return OAUTH2;
            case "visual_studio":
                return VISUAL_STUDIO;
            default:
                throw new InvalidConfigurationException(String.format("Invalid auth type '%s', supported values are: %s.", type,
                        Arrays.stream(values()).map(Object::toString).map(StringUtils::lowerCase).collect(Collectors.joining(", "))));
        }
    }
}
