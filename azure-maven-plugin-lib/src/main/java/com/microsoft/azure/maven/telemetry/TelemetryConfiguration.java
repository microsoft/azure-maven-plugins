/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.telemetry;

public interface TelemetryConfiguration {
    String getInstallationId();

    String getPluginName();

    String getPluginVersion();

    String getSubscriptionId();

    String getSessionId();
}
