/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.*;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.AbstractAzureResourceModule;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.postgre.model.PostgreSqlServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class AzurePostgreSql extends AbstractAzureResourceModule<PostgreSqlServer> implements AzureOperationEvent.Source<AzurePostgreSql> {
    private static final String POSTGRE_SQL_PROVIDER_AND_RESOURCE = "Microsoft.DBforPostgreSQL/servers";

    public AzurePostgreSql() {
        super(AzurePostgreSql::new);
    }

    private AzurePostgreSql(@Nonnull final List<Subscription> subscriptions) {
        super(AzurePostgreSql::new, subscriptions);
    }

    @Cacheable(cacheName = "postgre/{}", key = "$sid")
    @AzureOperation(name = "postgre.list.subscription", params = "sid", type = AzureOperation.Type.SERVICE)
    public List<PostgreSqlServer> list(@Nonnull String sid, boolean... force) {
        return getSubscriptions().stream()
            .map(subscription -> PostgreSqlManagerFactory.create(subscription.getId()))
            .flatMap(manager -> manager.servers().list().stream().map(server -> new PostgreSqlServer(manager, server)))
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    @Cacheable(cacheName = "postgre/{}/rg/{}/posgre/{}", key = "$sid/$rg/$name")
    @AzureOperation(name = "postgre.get_server.server|rg", params = {"name", "rg"}, type = AzureOperation.Type.SERVICE)
    public PostgreSqlServer get(@Nonnull String sid, @Nonnull String rg, @Nonnull String name) {
        final PostgreSqlManager postgreSqlManager = PostgreSqlManagerFactory.create(sid);
        final Server server = postgreSqlManager.servers().getByResourceGroup(rg, name);
        return new PostgreSqlServer(postgreSqlManager, server);
    }

    public PostgreSqlServer get(@Nonnull String id) {
        PostgreSqlManager manager = PostgreSqlManagerFactory.create(ResourceId.fromString(id).subscriptionId());
        final Server server = manager.servers().getById(id);
        return new PostgreSqlServer(manager, server);
    }

    @AzureOperation(name = "service.refresh", params = "this.name()", type = AzureOperation.Type.SERVICE)
    public void refresh() {
        try {
            CacheManager.evictCache("postgre/{}", CacheEvict.ALL);
        } catch (ExecutionException e) {
            log.warn("failed to evict cache", e);
        }
    }

    public PostgreSqlServer get(@Nonnull String resourceGroup, @Nonnull String name) {
        return get(getDefaultSubscription().getId(), resourceGroup, name);
    }

    public ICommittable<PostgreSqlServer> create(PostgreSqlServerConfig config) {
        return new Creator(config);
    }

    private static int getTierPriority(PerformanceTierProperties tier) {
        return StringUtils.equals("Basic", tier.id()) ? 1 :
            StringUtils.equals("GeneralPurpose", tier.id()) ? 2 : StringUtils.equals("MemoryOptimized", tier.id()) ? 3 : 4;
    }

    public CheckNameAvailabilityResultEntity checkNameAvailability(@Nonnull String subscriptionId, String name) {
        final PostgreSqlManager PostgreSqlManager = PostgreSqlManagerFactory.create(subscriptionId);
        final NameAvailabilityRequest request = new NameAvailabilityRequest().withName(name).withType(POSTGRE_SQL_PROVIDER_AND_RESOURCE);
        final NameAvailability result = PostgreSqlManager.checkNameAvailabilities().execute(request);

        return new CheckNameAvailabilityResultEntity(result.nameAvailable(), result.reason(), result.message());
    }

    public List<Region> listSupportedRegions() {
        return listSupportedRegions(getDefaultSubscription().getId());
    }

    public List<String> listSupportedVersions() {
        return ServerVersion.values().stream().map(ExpandableStringEnum::toString).sorted().collect(Collectors.toList());
    }

    public boolean checkRegionAvailability(@Nonnull String subscriptionId, @Nonnull Region region) {
        PostgreSqlManager manager = PostgreSqlManagerFactory.create(subscriptionId);
        List<PerformanceTierProperties> tiers = manager.locationBasedPerformanceTiers().list(region.getName()).stream().collect(Collectors.toList());
        return tiers.stream().anyMatch(e -> CollectionUtils.isNotEmpty(e.serviceLevelObjectives()));
    }

    private ServerVersion validateServerVersion(String version) {
        if (StringUtils.isNotBlank(version)) {
            final ServerVersion res = ServerVersion.fromString(version);
            if (res == null) {
                throw new AzureToolkitRuntimeException(String.format("Invalid postgre version '%s'.", version));
            }
            return res;
        }
        return null;
    }

    class Creator implements ICommittable<PostgreSqlServer>, AzureOperationEvent.Source<PostgreSqlServerConfig> {

        private final PostgreSqlServerConfig config;

        private final PostgreSqlManager manager;

        Creator(PostgreSqlServerConfig config) {
            this.config = config;
            this.manager = PostgreSqlManagerFactory.create(config.getSubscriptionId());
        }

        @Override
        @AzureOperation(name = "postgre.create_server", params = {"this.config.getName()"}, type = AzureOperation.Type.SERVICE)
        public PostgreSqlServer commit() {
            // retrieve sku
            ServerPropertiesForDefaultCreate parameters = new ServerPropertiesForDefaultCreate();
            parameters.withAdministratorLogin(config.getAdministratorLoginName())
                    .withAdministratorLoginPassword(config.getAdministratorLoginPassword())
                    .withVersion(validateServerVersion(config.getVersion()));

            final List<PerformanceTierProperties> tiers =
                    manager.locationBasedPerformanceTiers().list(config.getRegion().getName()).stream().collect(Collectors.toList());
            PerformanceTierProperties tier = tiers.stream().filter(e -> CollectionUtils.isNotEmpty(e.serviceLevelObjectives())).min((o1, o2) -> {
                int priority1 = getTierPriority(o1);
                int priority2 = getTierPriority(o2);
                return priority1 > priority2 ? 1 : -1;
            }).orElseThrow(() ->
                    new AzureToolkitRuntimeException("Currently, the service is not available in this location for your subscription."));
            Sku sku = new Sku().withName(tier.serviceLevelObjectives().get(0).id());
            // create server
            Server remote = manager.servers().define(config.getName())
                    .withRegion(config.getRegion().getName())
                    .withExistingResourceGroup(config.getResourceGroupName())
                    .withProperties(parameters)
                    .withSku(sku)
                    .create();
            PostgreSqlServer server = new PostgreSqlServer(this.manager, remote);
            // update firewall rules
            if (config.isEnableAccessFromAzureServices()) {
                server.firewallRules().enableAzureAccessRule();
            } else {
                server.firewallRules().disableAzureAccessRule();
            }
            // update common rule
            if (config.isEnableAccessFromLocalMachine()) {
                // TODO
                server.firewallRules().enableLocalMachineAccessRule(server.getPublicIpForLocalMachine());
            } else {
                server.firewallRules().disableLocalMachineAccessRule();
            }
            // refresh
            server.loadRemote();
            Azure.az(AzurePostgreSql.class).refresh();
            return server;
        }

        public AzureOperationEvent.Source<PostgreSqlServerConfig> getEventSource() {
            return new AzureOperationEvent.Source<PostgreSqlServerConfig>() {};
        }
    }

    public String name() {
        return POSTGRE_SQL_PROVIDER_AND_RESOURCE;
    }
}
