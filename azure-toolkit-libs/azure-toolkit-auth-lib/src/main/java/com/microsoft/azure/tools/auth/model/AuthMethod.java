/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.model;

/**
 * The actual auth method.
 */
public enum AuthMethod {
    AZURE_CLI,
    CLOUD_SHELL,
    OAUTH2,
    DEVICE_CODE,
    VISUAL_STUDIO,
    VSCODE,
    INTELLIJ_IDEA,
    MANAGED_IDENTITY,
    SERVICE_PRINCIPAL,
    AZURE_SECRET_FILE
}
