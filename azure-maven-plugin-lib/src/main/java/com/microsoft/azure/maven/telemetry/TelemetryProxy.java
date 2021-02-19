/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.telemetry;

import java.util.Map;

public interface TelemetryProxy {
    void trackEvent(final String eventName);

    void trackEvent(final String eventName, final Map<String, String> customProperties);

    void trackEvent(final String eventName, final Map<String, String> customProperties,
                    final boolean overrideDefaultProperties);

    void addDefaultProperty(final String key, final String value);

    Map<String, String> getDefaultProperties();

    void enable();

    void disable();
}
