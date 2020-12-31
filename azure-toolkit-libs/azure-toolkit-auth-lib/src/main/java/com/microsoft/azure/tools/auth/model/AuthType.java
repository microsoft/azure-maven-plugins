/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.model;

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
    OAUTH2,
}
