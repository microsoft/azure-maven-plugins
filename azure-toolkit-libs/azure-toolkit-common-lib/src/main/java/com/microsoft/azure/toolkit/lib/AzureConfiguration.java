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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
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
    String get(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key);

    void set(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, String value);

    @Nullable
    default <D> D get(Key<D> key) {
        //noinspection unchecked
        return (D) this.get(key.key);
    }

    @Nonnull
    default <D> D get(Key<D> key, @Nonnull D defaultValue) {
        final String v = this.get(key.key);
        //noinspection unchecked
        return StringUtils.isBlank(v) ? defaultValue : defaultValue.getClass().isInstance(v) ? (D) v : defaultValue;
    }

    default <D> void set(Key<D> key, D value) {
        this.set(key.key, value == null ? null : String.valueOf(value));
    }

    @Nullable
    default String getString(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
        final String v = this.get(key);
        return StringUtils.isBlank(v) ? null : v;
    }

    @Nullable
    default Boolean getBoolean(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
        final String v = this.get(key);
        return StringUtils.isBlank(v) ? null : Boolean.valueOf(v);
    }

    @Nullable
    default Integer getInteger(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
        final String v = this.get(key);
        return StringUtils.isBlank(v) ? null : Integer.valueOf(v);
    }

    @Nullable
    default Long getLong(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
        final String v = this.get(key);
        return StringUtils.isBlank(v) ? null : Long.valueOf(v);
    }

    @Nonnull
    default String getString(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, @Nonnull String defaultVal) {
        final String v = this.get(key);
        return StringUtils.isBlank(v) ? defaultVal : v;
    }

    @Nonnull
    default Boolean getBoolean(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, @Nonnull Boolean defaultVal) {
        final String v = this.get(key);
        return StringUtils.isBlank(v) ? defaultVal : Boolean.valueOf(v);
    }

    @Nonnull
    default Integer getInteger(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, @Nonnull Integer defaultVal) {
        final String v = this.get(key);
        return StringUtils.isBlank(v) ? defaultVal : Integer.valueOf(v);
    }

    @Nullable
    default Long getLong(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, @Nonnull Long defaultVal) {
        final String v = this.get(key);
        return StringUtils.isBlank(v) ? defaultVal : Long.valueOf(v);
    }

    Map<String, String> getSettings();

    void setSettings(Map<String, String> settings);

    default HttpLogDetailLevel getLogLevel() {
        return Optional.ofNullable(getString("common.log_level")).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
    }

    @Nonnull
    default HttpPipelinePolicy getUserAgentPolicy() {
        return new UserAgentPolicy(Azure.az().config().getString("common.user_agent"));
    }

    default List<String> getDocumentsLabelFields() {
        return Optional.ofNullable(getString("cosmos.documents_label_fields"))
            .filter(StringUtils::isNotBlank)
            .map(s -> Arrays.asList(s.split(";")))
            .orElse(DEFAULT_DOCUMENT_LABEL_FIELDS);
    }

    default String getEventHubsConsumerGroup() {
        return this.getString("event_hubs.consumer_group_name", "$Default");
    }

    default String getProduct() {
        return this.getString("common.product_name");
    }

    default String getVersion() {
        return this.getString("telemetry.telemetry_plugin_version");
    }

    default String getCloud() {
        return this.getString("account.azure_environment", "Azure");
    }

    default String getDotnetRuntimePath() {
        return this.getString("bicep.dotnet_runtime_path");
    }

    default int getPageSize() {
        return this.getInteger("common.page_size", DEFAULT_PAGE_SIZE);
    }

    default String getStorageExplorerPath() {
        return this.getString("storage.storage_explorer_path");
    }

    default Boolean getEnablePreloading() {
        return this.getBoolean("common.preloading_enabled", true);
    }

    default boolean isAuthPersistenceEnabled() {
        return this.getBoolean("other.enable_auth_persistence", true);
    }

    default Boolean getTelemetryEnabled() {
        return this.getBoolean("telemetry.telemetry_allow_telemetry", true);
    }

    default String getInstallationId() {
        return this.getString("telemetry.telemetry_installation_id");
    }

    default String getMachineId() {
        return this.getInstallationId();
    }

    default String getFunctionCoreToolsPath() {
        return this.getString("function.function_core_tools_path");
    }

    default Boolean getAuthPersistenceEnabled() {
        return this.getBoolean("other.enable_auth_persistence");
    }

    default String getDatabasePasswordSaveType() {
        return this.getString("database.password_save_type");
    }

    default String getUserAgent() {
        return this.getString("common.user_agent");
    }

    default Integer getMonitorQueryRowNumber() {
        return this.getInteger("monitor.monitor_table_rows");
    }

    default String getAzuritePath() {
        return this.getString("azurite.azurite_path");
    }

    default String getAzuriteWorkspace() {
        return this.getString("azurite.azurite_workspace");
    }

    default Boolean getEnableLeaseMode() {
        return this.getBoolean("azurite.enable_lease_mode");
    }

    default void setDotnetRuntimePath(String path) {
        this.set("bicep.dotnet_runtime_path", path);
    }

    default void setAuthPersistenceEnabled(Boolean enabled) {
        this.set("other.enable_auth_persistence", String.valueOf(enabled));
    }

    default void setFunctionCoreToolsPath(String path) {
        this.set("function.function_core_tools_path", path);
    }

    default void setInstallationId(String id) {
        this.set("telemetry.telemetry_installation_id", id);
    }

    default void setCloud(String v) {
        this.set("account.azure_environment", v);
    }

    default void setTelemetryEnabled(boolean v) {
        this.set("telemetry.telemetry_allow_telemetry", String.valueOf(v));
    }

    default void setDatabasePasswordSaveType(String v) {
        this.set("database.password_save_type", v);
    }

    default void setUserAgent(String v) {
        this.set("common.user_agent", v);
    }

    default void setStorageExplorerPath(String v) {
        this.set("storage.storage_explorer_path", v);
    }

    default void setPageSize(Integer v) {
        this.set("common.page_size", String.valueOf(v));
    }

    default void setDocumentsLabelFields(String v) {
        this.set("cosmos.documents_label_fields", v);
    }

    default void setDocumentsLabelFields(List<String> v) {
        this.set("cosmos.documents_label_fields", String.join(";", v));
    }

    default void setMonitorQueryRowNumber(Integer v) {
        this.set("monitor.monitor_table_rows", String.valueOf(v));
    }

    default void setEventHubsConsumerGroup(String v) {
        this.set("event_hubs.consumer_group_name", v);
    }

    default void setAzuritePath(String v) {
        this.set("azurite.azurite_path", v);
    }

    default void setAzuriteWorkspace(String v) {
        this.set("azurite.azurite_workspace", v);
    }

    default void setEnableLeaseMode(Boolean v) {
        this.set("azurite.enable_lease_mode", String.valueOf(v));
    }

    class Default implements AzureConfiguration {

        private final Map<String, String> settings = new HashMap<>();
        @Getter
        @Setter
        private ProxyInfo proxyInfo;
        @Getter
        @Setter
        private SSLContext sslContext;

        public Default() {
            this.set("account.azure_environment", "Azure");
            this.set("common.page_size", String.valueOf(DEFAULT_PAGE_SIZE));
            this.set("common.preloading_enabled", "true");
            this.set("telemetry.telemetry_allow_telemetry", "true");
            this.set("other.enable_auth_persistence", "true");
            this.set("event_hubs.consumer_group_name", "$Default");
            this.set("cosmos.documents_label_fields", String.join(";", DEFAULT_DOCUMENT_LABEL_FIELDS));
            this.set("database.password_save_type", "none");
            this.set("azurite.enable_lease_mode", "false");
            this.set("common.log_level", "NONE");
            this.set("monitor.monitor_table_rows", "200");
        }

        @Override
        public String get(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
            return settings.get(key);
        }

        public void set(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, String value) {
            settings.put(key, value);
        }

        public Map<String, String> getSettings() {
            return Collections.unmodifiableMap(settings);
        }

        public void setSettings(Map<String, String> settings) {
            this.settings.putAll(settings);
        }
    }

    @RequiredArgsConstructor
    class Key<D> {
        private final String key;

        public static <D> Key<D> of(String key) {
            return new Key<>(key);
        }
    }
}
