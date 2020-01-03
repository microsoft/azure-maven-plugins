/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.configuration;

public enum AuthType {
    AZURE_CLI,
    AZURE_CLI_SP,
    DEVICE_LOGIN,
    MSI,
    OAUTH,
    SECRET_FILE,
    SERVICE_PRINCIPAL,
    UNKNOWN
}
