/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import java.util.Map;

public interface AzureTelemetryConfigProvider {
    public Map<String, String> getCommonProperties();

    public String getEventNamePrefix();
}
