/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public enum AppInsightHelper implements TelemetryProxy {
    INSTANCE;

    private static final String TELEMETRY_KEY_SESSION_ID = "sessionId";
    private static final String TELEMETRY_KEY_INSTALLATIONID = "installationId";

    private boolean isEnabled = true;
    private String sessionId;
    private TelemetryClient client = new TelemetryClient();
    private Map<String, String> defaultProperties = new HashMap<>();

    AppInsightHelper() {
        sessionId = UUID.randomUUID().toString();
        defaultProperties.put(TELEMETRY_KEY_SESSION_ID, sessionId);
        defaultProperties.put(TELEMETRY_KEY_INSTALLATIONID, GetHashMac.getHashMac());
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void trackEvent(final String eventName) {
        trackEvent(eventName, null);
    }

    public void trackEvent(final String eventName, final Map<String, String> customProperties) {
        trackEvent(eventName, customProperties, false);
    }

    public void trackEvent(String eventName, Map<String, String> customProperties, boolean overrideDefaultProperties) {
        if (!isEnabled) {
            return;
        }
        final Map<String, String> properties = mergeProperties(customProperties, overrideDefaultProperties);
        client.trackEvent(eventName, properties, null);
        client.flush();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInstallationId() {
        return GetHashMac.getHashMac();
    }

    public void addDefaultProperty(String key, String value) {
        defaultProperties.put(key, value);
    }

    protected Map<String, String> mergeProperties(Map<String, String> customProperties,
                                                  boolean overrideDefaultProperties) {
        if (customProperties == null) {
            return defaultProperties;
        }

        final Map<String, String> baseMap = overrideDefaultProperties ? defaultProperties : customProperties;
        final Map<String, String> addMap = overrideDefaultProperties ? customProperties : defaultProperties;
        final Map<String, String> result = new HashMap<>(baseMap);
        result.putAll(addMap);

        return result;
    }

    @Override
    public Map<String, String> getDefaultProperties() {
        return defaultProperties;
    }
}

