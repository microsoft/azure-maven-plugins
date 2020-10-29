/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.channel.common.ApacheSenderFactory;
import com.microsoft.azure.common.utils.GetHashMac;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.azure.maven.telemetry.TelemetryConstants.TELEMETRY_EVENT_TELEMETRY_NOT_ALLOWED;
import static com.microsoft.azure.maven.telemetry.TelemetryConstants.TELEMETRY_KEY_INSTALLATIONID;
import static com.microsoft.azure.maven.telemetry.TelemetryConstants.TELEMETRY_KEY_SESSION_ID;

public enum AppInsightHelper implements TelemetryProxy {
    INSTANCE;

    private boolean isEnabled = true;
    private String sessionId;
    private TelemetryClient client = new TelemetryClient();
    private Map<String, String> defaultProperties = new HashMap<>();

    AppInsightHelper() {
        sessionId = UUID.randomUUID().toString();
        defaultProperties.put(TELEMETRY_KEY_SESSION_ID, sessionId);
        defaultProperties.put(TELEMETRY_KEY_INSTALLATIONID, GetHashMac.getHashMac());
        initTelemetryHttpClient();
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        trackEvent(TELEMETRY_EVENT_TELEMETRY_NOT_ALLOWED);
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

    // When maven goal executes too quick, The HTTPClient of AI SDK may not fully initialized and will step
    // into endless loop when close, we need to call it in main thread.
    // Refer here for detail codes: https://github.com/Microsoft/ApplicationInsights-Java/blob/master/core/src
    // /main/java/com/microsoft/applicationinsights/internal/channel/common/ApacheSender43.java#L103
    public void initTelemetryHttpClient() {
        try {
            ApacheSenderFactory.INSTANCE.create().getHttpClient();
        } catch (Exception e) {
            // swallow this exception
        }
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

