/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Getter
public class AzureTelemetryClient {

    public static final String CONFIGURATION_FILE = "ApplicationInsights.xml";
    public static final Pattern INSTRUMENTATION_KEY_PATTERN = Pattern.compile("<InstrumentationKey>(.*)</InstrumentationKey>");

    private final TelemetryClient client;
    @Setter
    private Map<String, String> defaultProperties;
    private boolean isEnabled = true;     // Telemetry is enabled by default.

    public AzureTelemetryClient() {
        this(Collections.emptyMap());
    }

    public AzureTelemetryClient(@Nonnull final Map<String, String> defaultProperties) {
        this.client = new TelemetryClient();
        this.defaultProperties = new HashMap<>(defaultProperties);
    }

    public void addDefaultProperty(String key, String value) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        defaultProperties.put(key, value);
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void trackEvent(final String eventName) {
        trackEvent(eventName, null, null, false);
    }

    public void trackEvent(final String eventName, final Map<String, String> customProperties) {
        trackEvent(eventName, customProperties, null, false);
    }

    public void trackEvent(final String eventName, final Map<String, String> customProperties, final Map<String, Double> metrics) {
        trackEvent(eventName, customProperties, metrics, false);
    }

    public void trackEvent(final String eventName, final Map<String, String> customProperties, final Map<String, Double> metrics,
                           final boolean overrideDefaultProperties) {
        if (!isEnabled()) {
            return;
        }

        final Map<String, String> properties = mergeProperties(getDefaultProperties(), customProperties, overrideDefaultProperties);

        client.trackEvent(eventName, properties, metrics);
        client.flush();
    }

    protected Map<String, String> mergeProperties(Map<String, String> defaultProperties,
                                                  Map<String, String> customProperties,
                                                  boolean overrideDefaultProperties) {
        if (customProperties == null) {
            return defaultProperties;
        }

        final Map<String, String> merged = new HashMap<>();
        if (overrideDefaultProperties) {
            merged.putAll(defaultProperties);
            merged.putAll(customProperties);
        } else {
            merged.putAll(customProperties);
            merged.putAll(defaultProperties);
        }
        merged.entrySet().removeIf(stringStringEntry -> StringUtils.isEmpty(stringStringEntry.getValue()));
        return merged;
    }
}
