/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.telemetry;

import java.util.Map;

public interface TelemetryConfiguration {
    Map<String, String> getTelemetryProperties();
}
