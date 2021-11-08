/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.*;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.mysql.model.MySqlServerConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class AzureMySql extends SubscriptionScoped<AzureMySql> implements AzureService {
    private static final String MYSQL_PROVIDER_AND_RESOURCE = "Microsoft.DBforMySQL/servers";

    public AzureMySql() {
        super(AzureMySql::new);
    }

    private AzureMySql(@Nonnull final List<Subscription> subscriptions) {
        super(AzureMySql::new, subscriptions);
    }

    public List<MySqlServer> list() {
        return getSubscriptions().stream()
            .map(subscription -> MySqlManagerFactory.create(subscription.getId()))
            .flatMap(manager -> manager.servers().list().stream().map(server -> new MySqlServer(manager, server)))
            .collect(Collectors.toList());
    }

    public MySqlServer get(String id) {
        MySqlManager manager = MySqlManagerFactory.create(ResourceId.fromString(id).subscriptionId());
        final Server server = manager.servers().getById(id);
        return new MySqlServer(manager, server);
    }

    public MySqlServer get(final String resourceGroup, final String name) {
        MySqlManager manager = MySqlManagerFactory.create(getDefaultSubscription().getId());
        final Server server = manager.servers().getByResourceGroup(resourceGroup, name);
        return new MySqlServer(manager, server);
    }

    public ICommittable<MySqlServer> create(MySqlServerConfig config) {
        return new Creator(config);
    }

    private static int getTierPriority(PerformanceTierProperties tier) {
        return StringUtils.equals("Basic", tier.id()) ? 1 :
            StringUtils.equals("GeneralPurpose", tier.id()) ? 2 : StringUtils.equals("MemoryOptimized", tier.id()) ? 3 : 4;
    }

    public CheckNameAvailabilityResultEntity checkNameAvailability(@Nonnull String subscriptionId, @Nonnull String name) {
        final MySqlManager mySqlManager = MySqlManagerFactory.create(subscriptionId);
        final NameAvailabilityRequest request = new NameAvailabilityRequest().withName(name).withType(MYSQL_PROVIDER_AND_RESOURCE);
        final NameAvailability result = mySqlManager.checkNameAvailabilities().execute(request);
        return new CheckNameAvailabilityResultEntity(result.nameAvailable(), result.reason(), result.message());
    }

    public List<Region> listSupportedRegions() {
        return listSupportedRegions(getDefaultSubscription().getId());
    }

    public List<String> listSupportedVersions() {
        return ServerVersion.values().stream().map(ExpandableStringEnum::toString).collect(Collectors.toList());
    }

    public boolean checkRegionAvailability(Region region) {
        MySqlManager mySqlManager = MySqlManagerFactory.create(getDefaultSubscription().getId());
        List<PerformanceTierProperties> tiers = mySqlManager.locationBasedPerformanceTiers().list(region.getName()).stream().collect(Collectors.toList());
        return tiers.stream().anyMatch(e -> CollectionUtils.isNotEmpty(e.serviceLevelObjectives()));
    }

    private ServerVersion validateServerVersion(String version) {
        if (StringUtils.isNotBlank(version)) {
            final ServerVersion res = ServerVersion.fromString(version);
            if (res == null) {
                throw new AzureToolkitRuntimeException(String.format("Invalid mysql version '%s'.", version));
            }
            return res;
        }
        return null;
    }

    class Creator implements ICommittable<MySqlServer>, AzureOperationEvent.Source<MySqlServerConfig> {

        private final MySqlServerConfig config;

        private final MySqlManager manager;

        Creator(MySqlServerConfig config) {
            this.config = config;
            this.manager = MySqlManagerFactory.create(config.getSubscriptionId());
        }

        @Override
        @AzureOperation(name = "mysql|server.create", params = {"this.config.getName()"}, type = AzureOperation.Type.SERVICE)
        public MySqlServer commit() {
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
            MySqlServer server = new MySqlServer(this.manager, remote);
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
            return server;
        }

        public AzureOperationEvent.Source<MySqlServerConfig> getEventSource() {
            return new AzureOperationEvent.Source<MySqlServerConfig>() {};
        }
    }

    public String name() {
        return MYSQL_PROVIDER_AND_RESOURCE;
    }
}
