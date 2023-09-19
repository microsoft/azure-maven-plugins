/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.toolkit.lib.Azure;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
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

    @Nonnull
    private final TelemetryClient client = new TelemetryClient();
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String eventNamePrefix;
    @Nonnull
    private final Map<String, String> defaultProperties = new HashMap<String, String>() {
        {
            put(ARCH_KEY, System.getProperty("os.arch"));
            put(JDK_KEY, System.getProperty("java.version"));
        }
    };

    public AzureTelemetryClient() {
        final AzureTelemetryConfigProvider provider = loadConfigProvider();
        if (Objects.nonNull(provider)) {
            this.defaultProperties.putAll(provider.getCommonProperties());
            this.eventNamePrefix = provider.getEventNamePrefix();
        } else {
            this.eventNamePrefix = "AzurePlugin";
        }
    }

    @Nullable
    private static AzureTelemetryConfigProvider loadConfigProvider() {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(AzureTelemetryClient.class.getClassLoader());
            final ServiceLoader<AzureTelemetryConfigProvider> loader = ServiceLoader.load(AzureTelemetryConfigProvider.class, AzureTelemetryClient.class.getClassLoader());
            final Iterator<AzureTelemetryConfigProvider> iterator = loader.iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            }
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    public void addDefaultProperty(@Nonnull String key, @Nonnull String value) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        defaultProperties.put(key, value);
    }

    public void addDefaultProperties(@Nonnull Map<String, String> properties) {
        defaultProperties.putAll(properties);
    }

    public boolean isEnabled() {
        return BooleanUtils.isNotFalse(Azure.az().config().getTelemetryEnabled());
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
            return Arrays.stream(value.split("\\r?\\n"))
                .map(AzureTelemetryClient::anonymizePiiData).collect(Collectors.joining(StringUtils.LF));
        });
    }

    public static String anonymizePiiData(@Nonnull final String input) {
        final String result = FILE_PATH_PATTERN.matcher(input).replaceAll("<REDACTED: user-file-path>");
        for (final Pattern pattern : PATTERN_MAP.keySet()) {
            if (pattern.matcher(result).find()) {
                return PATTERN_MAP.get(pattern);
            }
        }
        return result;
    }
}
