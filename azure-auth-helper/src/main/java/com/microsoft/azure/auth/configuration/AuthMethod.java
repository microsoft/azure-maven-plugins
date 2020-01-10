/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.configuration;

/**
 * Enum records detail auth method
 */
public enum AuthMethod {
    AZURE_CLI(AuthType.AZURE_CLI),
    AZURE_SECRET_FILE(AuthType.AZURE_AUTH_MAVEN_PLUGIN),
    CLOUD_SHELL(AuthType.AZURE_CLI),
    DEVICE_LOGIN(AuthType.AZURE_AUTH_MAVEN_PLUGIN),
    OAUTH(AuthType.AZURE_AUTH_MAVEN_PLUGIN),
    SERVICE_PRINCIPAL(AuthType.SERVICE_PRINCIPAL);

    private AuthType authType;

    AuthMethod(AuthType authType) {
        this.authType = authType;
    }

    public AuthType getAuthType() {
        return authType;
    }
}
