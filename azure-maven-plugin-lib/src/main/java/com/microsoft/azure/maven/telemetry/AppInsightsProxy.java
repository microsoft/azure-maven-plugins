/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.concrete.TelemetryChannelBase;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import org.codehaus.plexus.util.StringUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AppInsightsProxy implements TelemetryProxy {
    protected TelemetryClient client;

    protected TelemetryConfiguration configuration;

    protected Map<String, String> defaultProperties;

    // Telemetry is enabled by default.
    protected boolean isEnabled = true;

    public AppInsightsProxy(final TelemetryConfiguration config) {
        client = new TelemetryClient(readConfigurationFromFile());
        if (config == null) {
            throw new NullPointerException();
        }
        configuration = config;
        defaultProperties = configuration.getTelemetryProperties();
    }

    /**
     * This is a workaround for telemetry issue. ApplicationInsight read configuration file by JAXB, and JAXB parse
     * configuration by JAXBContext, but the context model differs in Java 8 and Java 11 during maven execution, so
     * read the config file by maven plugin here.
     */
    private com.microsoft.applicationinsights.TelemetryConfiguration readConfigurationFromFile() {
        final com.microsoft.applicationinsights.TelemetryConfiguration telemetryConfiguration =
            new com.microsoft.applicationinsights.TelemetryConfiguration();
        final Map<String, String> channelProperties = new HashMap<>();
        channelProperties.put(TelemetryChannelBase.FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME, "1");
        final TelemetryChannel channel = new InProcessTelemetryChannel(channelProperties);

        telemetryConfiguration.setChannel(channel);
        telemetryConfiguration.setInstrumentationKey(readInstrumentationKeyFromConfiguration());
        return telemetryConfiguration;
    }

    private String readInstrumentationKeyFromConfiguration() {
        final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("ApplicationInsights.xml");
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document configuration = builder.parse(inputStream);
            return configuration.getDocumentElement().getElementsByTagName("InstrumentationKey")
                .item(0).getTextContent();
        } catch (Exception e) {
            return "";
        }
    }

    public void addDefaultProperty(String key, String value) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        defaultProperties.put(key, value);
    }

    public Map<String, String> getDefaultProperties() {
        return defaultProperties;
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void trackEvent(final String eventName) {
        trackEvent(eventName, null, false);
    }

    public void trackEvent(final String eventName, final Map<String, String> customProperties) {
        trackEvent(eventName, customProperties, false);
    }

    public void trackEvent(final String eventName, final Map<String, String> customProperties,
                           final boolean overrideDefaultProperties) {
        if (!isEnabled) {
            return;
        }

        final Map<String, String> properties = mergeProperties(getDefaultProperties(), customProperties,
            overrideDefaultProperties);

        client.trackEvent(eventName, properties, null);
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
        final Iterator<Map.Entry<String, String>> it = merged.entrySet().iterator();
        while (it.hasNext()) {
            if (StringUtils.isEmpty(it.next().getValue())) {
                it.remove();
            }
        }
        return merged;
    }
}
