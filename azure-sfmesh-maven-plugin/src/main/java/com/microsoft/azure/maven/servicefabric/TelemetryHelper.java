/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.servicefabric;

import com.microsoft.applicationinsights.TelemetryClient;

import org.apache.maven.plugin.logging.Log;
import java.util.HashMap;
import java.util.Map;

public class TelemetryHelper {
    private static final TelemetryClient client = new TelemetryClient();

    public static boolean sendEvent(TelemetryEventType type, String value, Log logger){
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("Description", value);
        try {
            client.trackEvent(type.getValue(), properties, null);
            client.flush();
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            logger.error(String.format("Failed sending telemetry event of type %s", type.getValue()));
        }
        return true;
    }
}
