/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.cassandra;

import com.azure.resourcemanager.cosmos.fluent.CassandraResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.CassandraTableGetResultsInner;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.cosmos.model.CassandraDatabaseAccountConnectionString;
import com.microsoft.azure.toolkit.lib.cosmos.model.ThroughputConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class CassandraTableDraft extends CassandraTable implements
        AzResource.Draft<CassandraTable, CassandraTableGetResultsInner> {

    @Setter
    private CassandraTableConfig config;

    protected CassandraTableDraft(@NotNull String name, @NotNull String resourceGroupName, @NotNull CassandraTableModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @NotNull
    @Override
    public CassandraTableGetResultsInner createResourceInAzure() {
        final CassandraKeyspace keyspace = getParent();
        final CassandraCosmosDBAccount account = (CassandraCosmosDBAccount) keyspace.getParent();

        try (final CqlSession session = createSession(account)) {
            AzureMessager.getMessager().info(AzureString.format("Start creating Cassandra table({0})...", this.getName()));
            session.execute(String.format("CREATE TABLE %s.%s %s", keyspace.getName(), getName(), ensureConfig().getSchema()));
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new AzureToolkitRuntimeException("Failed to create Cassandra table.", e);
        }
        final CassandraResourcesClient cassandraResourcesClient = Objects.requireNonNull(((CassandraTableModule) Objects.requireNonNull(getModule())).getClient());
        AzureMessager.getMessager().success(AzureString.format("Cassandra table({0}) is successfully created.", this.getName()));
        return cassandraResourcesClient.getCassandraTable(this.getResourceGroupName(), account.getName(), keyspace.getName(), this.getName());
    }

    private CqlSession createSession(final CassandraCosmosDBAccount account) throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, null, new java.security.SecureRandom());
        final DriverConfigLoader configLoader =
                DriverConfigLoader.programmaticBuilder().withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(10)).build();
        final CassandraDatabaseAccountConnectionString connectionString = Objects.requireNonNull(account.getCassandraConnectionString());
        return CqlSession.builder()
                .withLocalDatacenter(Objects.requireNonNull(account.getRegion()).getLabel())
                .withConfigLoader(configLoader)
                .withSslContext(sc)
                .addContactPoint(new InetSocketAddress(connectionString.getHost(), connectionString.getPort()))
                .withAuthCredentials(connectionString.getUsername(), connectionString.getPassword()).build();
    }

    @NotNull
    @Override
    public CassandraTableGetResultsInner updateResourceInAzure(@NotNull CassandraTableGetResultsInner origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return config != null && ObjectUtils.anyNotNull(config.getTableId(), config.getSchema());
    }

    @Nullable
    @Override
    public CassandraTable getOrigin() {
        return null;
    }

    private CassandraTableConfig ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(CassandraTableConfig::new);
        return this.config;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CassandraTableConfig extends ThroughputConfig {
        public static final String DEFAULT_SCHEMA = "(userid int, name text, email text, PRIMARY KEY (userid))";
        private String tableId;
        private String schema;

        public static CassandraTableConfig getDefaultConfig() {
            final CassandraTableConfig result = new CassandraTableConfig();
            result.setTableId(String.format("table%s", Utils.getTimestamp()));
            result.setSchema(DEFAULT_SCHEMA);
            return result;
        }
    }
}
