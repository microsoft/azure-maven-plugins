/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.microsoft.azure.toolkit.lib.auth.cli.AzureCliAccount;
import com.microsoft.azure.toolkit.lib.auth.devicecode.DeviceCodeAccount;
import com.microsoft.azure.toolkit.lib.auth.managedidentity.ManagedIdentityAccount;
import com.microsoft.azure.toolkit.lib.auth.oauth.OAuthAccount;
import com.microsoft.azure.toolkit.lib.common.exception.InvalidConfigurationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The auth type which user may define in configuration.
 */
@Getter
@RequiredArgsConstructor
public enum AuthType {
    AUTO("Auto"),
    SERVICE_PRINCIPAL("Service Principal"),
    AZURE_AUTH_MAVEN_PLUGIN("Maven Plugin"),
    MANAGED_IDENTITY("Managed Identity"),
    AZURE_CLI("Azure CLI"),
    VSCODE("VSCode"),
    INTELLIJ_IDEA("IntelliJ IDEA"),
    VISUAL_STUDIO("Visual Studio"),
    DEVICE_CODE("Device Code"),
    OAUTH2("OAuth2");

    private final String label;

    @Nonnull
    public static AuthType parseAuthType(String type) throws InvalidConfigurationException {
        if (StringUtils.isBlank(type)) {
            return AUTO;
        }
        switch (StringUtils.replace(type.toLowerCase().trim(), "-", "_")) {
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

    public boolean checkAvailable() {
        return this.checkAvailable(new AuthConfiguration(this));
    }

    public boolean checkAvailable(final AuthConfiguration config) {
        switch (this) {
            case AUTO:
                return true;
            case MANAGED_IDENTITY:
                return new ManagedIdentityAccount(config).checkAvailable();
            case AZURE_CLI:
                return new AzureCliAccount(config).checkAvailable();
            case OAUTH2:
                return new OAuthAccount(config).checkAvailable();
            case DEVICE_CODE:
                return new DeviceCodeAccount(config).checkAvailable();
            default:
                return false;
        }
    }
}
