/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.UserAgentPolicy;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AzureConfiguration {
    List<String> DEFAULT_DOCUMENT_LABEL_FIELDS = Arrays.asList("name", "Name", "NAME", "ID", "UUID", "Id", "id", "uuid");
    int DEFAULT_PAGE_SIZE = 99;

    @Nullable
    ProxyInfo getProxyInfo();

    void setProxyInfo(ProxyInfo proxy);

    @Nullable
    SSLContext getSslContext();

    void setSslContext(SSLContext context);

    @Nullable
    Object get(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key);

    void set(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, Object value);

    @Nullable
    default String getString(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
        final Object v = this.get(key);
        return isBlank(v) ? null : v instanceof String ? (String) v : null;
    }

    @Nullable
    default Boolean getBoolean(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
        final Object v = this.get(key);
        return isBlank(v) ? null : v instanceof Boolean ? (Boolean) v : null;
    }

    @Nullable
    default Integer getInteger(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
        final Object v = this.get(key);
        return isBlank(v) ? null : v instanceof Integer ? (Integer) v : null;
    }

    @Nonnull
    default String getString(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, @Nonnull String defaultVal) {
        final Object v = this.get(key);
        return isBlank(v) ? defaultVal : v instanceof String ? (String) v : defaultVal;
    }

    @Nonnull
    default Boolean getBoolean(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, @Nonnull Boolean defaultVal) {
        final Object v = this.get(key);
        return isBlank(v) ? defaultVal : v instanceof Boolean ? (Boolean) v : defaultVal;
    }

    @Nonnull
    default Integer getInteger(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, @Nonnull Integer defaultVal) {
        final Object v = this.get(key);
        return isBlank(v) ? defaultVal : v instanceof Integer ? (Integer) v : defaultVal;
    }

    @Contract("null -> true")
    static boolean isBlank(@Nullable Object str) {
        return ObjectUtils.isEmpty(str) || (str instanceof CharSequence && StringUtils.isBlank((CharSequence) str));
    }

    default HttpLogDetailLevel getLogLevel() {
        return Optional.ofNullable(getString("common.log_level")).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
    }

    @Nonnull
    default HttpPipelinePolicy getUserAgentPolicy() {
        return new UserAgentPolicy(Azure.az().config().getString("common.user_agent"));
    }

    default List<String> getDocumentsLabelFields() {
        return Optional.ofNullable(getString("cosmos.document_label_fields"))
            .filter(StringUtils::isNotBlank)
            .map(s -> Arrays.asList(s.split(";")))
            .orElse(DEFAULT_DOCUMENT_LABEL_FIELDS);
    }

    default String getEventHubsConsumerGroup() {
        return this.getString("event_hubs.default_consumer_group", "$Default");
    }

    default String getProduct() {
        return this.getString("common.product_name");
    }

    default String getVersion() {
        return this.getString("common.product_version");
    }

    default String getCloud() {
        return this.getString("common.cloud", "Azure");
    }

    default int getPageSize() {
        return this.getInteger("common.page_size", DEFAULT_PAGE_SIZE);
    }

    default Boolean getEnablePreloading() {
        return this.getBoolean("common.preloading_enabled", true);
    }

    default boolean isAuthPersistenceEnabled() {
        return this.getBoolean("auth.cache_enabled", true);
    }

    default Boolean getTelemetryEnabled() {
        return this.getBoolean("telemetry.enabled", true);
    }

    class Default implements AzureConfiguration {

        private final Map<String, Object> persistent = new HashMap<>();
        @Getter
        @Setter
        private ProxyInfo proxyInfo;
        @Getter
        @Setter
        private SSLContext sslContext;

        public Default() {
            this.set("common.cloud", "Azure");
            this.set("common.page_size", DEFAULT_PAGE_SIZE);
            this.set("common.preloading_enabled", true);
            this.set("telemetry.enabled", true);
            this.set("auth.cache_enabled", true);
            this.set("event_hubs.default_consumer_group", "$Default");
            this.set("cosmos.document_label_fields", String.join(";", DEFAULT_DOCUMENT_LABEL_FIELDS));
            this.set("database.password_save_type", "none");
            this.set("storage.azurite_lease_mode_enabled", false);
            this.set("common.log_level", "NONE");
            this.set("monitor.page_size", 200);
        }

        @Override
        public Object get(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
            return persistent.get(key);
        }

        public void set(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, Object value) {
            persistent.put(key, value);
        }

        public Map<String, Object> getPersistent() {
            return Collections.unmodifiableMap(persistent);
        }
    }
}
