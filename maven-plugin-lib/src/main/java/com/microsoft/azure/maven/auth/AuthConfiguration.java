/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.maven.AuthenticationSetting;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;

public interface AuthConfiguration {
    Settings getSettings();

    Log getLog();

    String getSubscriptionId();

    String getUserAgent();

    AuthenticationSetting getAuthenticationSetting();
}
