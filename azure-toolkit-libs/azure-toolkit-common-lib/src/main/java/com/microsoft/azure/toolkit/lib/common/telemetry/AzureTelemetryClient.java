/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.concrete.TelemetryChannelBase;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
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
        this(Collections.EMPTY_MAP);
    }

    public AzureTelemetryClient(@Nonnull final Map<String, String> defaultProperties) {
        this.client = new TelemetryClient();
        this.defaultProperties = new HashMap<>(defaultProperties);
    }

    //      This is a workaround for telemetry issue. ApplicationInsight read configuration file by JAXB, and JAXB parse
    //      configuration by JAXBContext, but the context model differs in Java 8 and Java 11 during maven execution,
    //      here is the link https://github.com/Microsoft/ApplicationInsights-Java/issues/674, will remove the code and
    //      use ai sdk to read configuration file once the issue is fixed
    private com.microsoft.applicationinsights.TelemetryConfiguration readConfigurationFromFile() {
        final com.microsoft.applicationinsights.TelemetryConfiguration telemetryConfiguration =
            new com.microsoft.applicationinsights.TelemetryConfiguration();
        final Map<String, String> channelProperties = new HashMap<>();
        channelProperties.put(TelemetryChannelBase.FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME, "1");
        final TelemetryChannel channel = new InProcessTelemetryChannel(channelProperties);
        telemetryConfiguration.setChannel(channel);

        final String key = readInstrumentationKeyFromConfiguration();
        if (StringUtils.isNotEmpty(key)) {
            telemetryConfiguration.setInstrumentationKey(key);
        } else {
            // No instrumentation key found.
            disable();
        }

        TelemetryConfigurationFactory.INSTANCE.initialize(telemetryConfiguration);
        return telemetryConfiguration;
    }

    // Get instrumentation key from ApplicationInsights.xml
    private String readInstrumentationKeyFromConfiguration() {
        try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(CONFIGURATION_FILE)) {
            if (inputStream == null) {
                return StringUtils.EMPTY;
            }

            final String configurationContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            final Matcher matcher = INSTRUMENTATION_KEY_PATTERN.matcher(configurationContent);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return StringUtils.EMPTY;
            }
        } catch (IOException exception) {
            return StringUtils.EMPTY;
        }
    }
    // end

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
