/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.auth;

import org.apache.maven.settings.Settings;

public interface AuthConfiguration {
    Settings getSettings();

    String getSubscriptionId();

    String getUserAgent();

    String getHttpProxyHost();

    int getHttpProxyPort();

    AuthenticationSetting getAuthenticationSetting();
}
