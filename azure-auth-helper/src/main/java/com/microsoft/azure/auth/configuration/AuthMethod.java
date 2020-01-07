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
    AZURE_CLI,
    DEVICE_LOGIN,
    CLOUD_SHELL,
    OAUTH,
    AZURE_SECRET_FILE,
    SERVICE_PRINCIPAL
}
