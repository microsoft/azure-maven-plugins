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
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.action.Action.RESOURCE_TYPE;

@Getter
public class AzureTelemetryClient {
    public static final String ARCH_KEY = "arch";
    public static final String JDK_KEY = "jdk";

    private static final String[] SYSTEM_PROPERTIES = new String[]{RESOURCE_TYPE};
    // refers https://github.com/microsoft/vscode-extension-telemetry/blob/main/src/telemetryReporter.ts
    private static final String FILE_PATH_REGEX =
            "(file://)?([a-zA-Z]:(\\\\\\\\|\\\\|/)|(\\\\\\\\|\\\\|/))?([\\w-._]+(\\\\\\\\|\\\\|/))+[\\w-._]*";
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile(FILE_PATH_REGEX);
    // refers https://github.com/microsoft/vscode-extension-telemetry/blob/v0.6.2/src/common/baseTelemetryReporter.ts#L241
    private static final Pattern GOOGLE_API_KEY = Pattern.compile("AIza[a-zA-Z0-9_\\\\-]{35}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-]+");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(key|token|sig|secret|signature|password|passwd|pwd|android:value)[^a-zA-Z0-9]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_REGEX = Pattern.compile("xox[pbar]-[a-zA-Z0-9]", Pattern.CASE_INSENSITIVE);

    private static final Map<Pattern, String> PATTERN_MAP = new HashMap<Pattern, String>() {{
        put(EMAIL_PATTERN, "<REDACTED: Email>");
        put(SECRET_PATTERN, "<REDACTED: Generic Secret>");
        put(TOKEN_REGEX, "<REDACTED: Slack Toke>");
        put(GOOGLE_API_KEY, "<REDACTED: Google API Key>");
    }};

    private final TelemetryClient client;
    @Setter
    private Map<String, String> defaultProperties;
    private boolean isEnabled = true;     // Telemetry is enabled by default.

    public AzureTelemetryClient() {
        this(Collections.emptyMap());
    }

    public AzureTelemetryClient(@Nonnull final Map<String, String> defaultProperties) {
        this.client = new TelemetryClient();
        this.defaultProperties = new HashMap<>();
        initDefaultProperties();
        this.defaultProperties.putAll(defaultProperties);
    }

    public void addDefaultProperty(@Nonnull String key, @Nonnull String value) {
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

    public void trackEvent(@Nonnull final String eventName) {
        trackEvent(eventName, null, null, false);
    }

    public void trackEvent(@Nonnull final String eventName, @Nullable final Map<String, String> customProperties) {
        trackEvent(eventName, customProperties, null, false);
    }

    public void trackEvent(@Nonnull final String eventName, @Nullable final Map<String, String> customProperties, @Nullable final Map<String, Double> metrics) {
        trackEvent(eventName, customProperties, metrics, false);
    }

    public void trackEvent(@Nonnull final String eventName, @Nullable final Map<String, String> customProperties, @Nullable final Map<String, Double> metrics,
                           final boolean overrideDefaultProperties) {
        if (!isEnabled()) {
            return;
        }

        final Map<String, String> properties = mergeProperties(getDefaultProperties(), customProperties, overrideDefaultProperties);
        properties.entrySet().removeIf(stringStringEntry -> StringUtils.isEmpty(stringStringEntry.getValue())); // filter out null values
        anonymizePersonallyIdentifiableInformation(properties);
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
        return merged;
    }

    protected static void anonymizePersonallyIdentifiableInformation(final Map<String, String> properties) {
        properties.replaceAll((key, value) -> {
            if (StringUtils.isBlank(value) || StringUtils.equalsAnyIgnoreCase(key, SYSTEM_PROPERTIES)) {
                return value;
            }
            return Arrays.stream(value.split("\\r?\\n")).map(line -> {
                final String input = FILE_PATH_PATTERN.matcher(line).replaceAll("<REDACTED: user-file-path>");
                for (final Pattern pattern : PATTERN_MAP.keySet()) {
                    if (pattern.matcher(input).find()) {
                        return PATTERN_MAP.get(pattern);
                    }
                }
                return input;
            }).collect(Collectors.joining(StringUtils.LF));
        });
    }

    private void initDefaultProperties() {
        this.addDefaultProperty(ARCH_KEY, System.getProperty("os.arch"));
        this.addDefaultProperty(JDK_KEY, System.getProperty("java.version"));
    }
}
