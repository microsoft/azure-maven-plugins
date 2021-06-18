/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.mysql.service;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.NameAvailabilityRequest;
import com.azure.resourcemanager.mysql.models.PerformanceTierProperties;
import com.azure.resourcemanager.mysql.models.Server;
import com.azure.resourcemanager.mysql.models.ServerPropertiesForDefaultCreate;
import com.azure.resourcemanager.mysql.models.ServerVersion;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class AzureMySql extends SubscriptionScoped<AzureMySql> implements AzureService {
    private static final List<String> MYSQL_SUPPORTED_REGIONS = Arrays.asList(
        "australiacentral", "australiacentral2", "australiaeast", "australiasoutheast", "brazilsouth", "canadacentral", "canadaeast", "centralindia",
        "centralus", "eastasia", "eastus2", "eastus", "francecentral", "francesouth", "germanywestcentral", "japaneast", "japanwest", "koreacentral",
        "koreasouth", "northcentralus", "northeurope", "southafricanorth", "southafricawest", "southcentralus", "southindia", "southeastasia",
        "norwayeast", "switzerlandnorth", "uaenorth", "uksouth", "ukwest", "westcentralus", "westeurope", "westindia", "westus", "westus2",
        "centraluseuap", "eastus2euap");
    private static final String NAME_AVAILABILITY_CHECK_TYPE = "Microsoft.DBforMySQL/servers";

    public AzureMySql() {
        super(AzureMySql::new);
    }

    private AzureMySql(@Nonnull final List<Subscription> subscriptions) {
        super(AzureMySql::new, subscriptions);
    }

    public List<MySqlServer> list() {
        return getSubscriptions().stream()
            .map(subscription -> MySqlManagerFactory.create(subscription.getId()))
            .flatMap(manager -> manager.servers().list().stream())
            .collect(Collectors.toList()).stream()
            .map(this::toMysqlServer)
            .collect(Collectors.toList());
    }

    public MySqlServer get(String id) {
        final Server server = MySqlManagerFactory.create(ResourceId.fromString(id).subscriptionId()).servers().getById(id);
        return toMysqlServer(server);
    }

    public MySqlServer get(final String resourceGroup, final String name) {
        final Server server = MySqlManagerFactory.create(getDefaultSubscription().getId()).servers().getByResourceGroup(resourceGroup, name);
        return toMysqlServer(server);
    }

    public AbstractMySqlCreator create() {
        final MySqlManager manager = MySqlManagerFactory.create(getDefaultSubscription().getId());
        return new AbstractMySqlCreator() {

            private MySqlServer app;
            @Override
            @AzureOperation(name = "mysql|server.create", params = {"this.app.name()"}, type = AzureOperation.Type.SERVICE)
            public MySqlServer commit() {

                ServerPropertiesForDefaultCreate parameters = new ServerPropertiesForDefaultCreate();
                parameters.withAdministratorLogin(this.getAdministratorLogin())
                    .withAdministratorLoginPassword(this.getAdministratorLoginPassword())
                    .withVersion(validateServerVersion(this.getVersion()));
                Server server = manager.servers().define(getName())
                    .withRegion(getRegion().getName())
                    .withExistingResourceGroup(getResourceGroupName())
                    .withProperties(parameters).create();
                this.app = toMysqlServer(server);
                return app;
            }

            @NotNull
            @Override
            public AzureOperationEvent.Source<MySqlServer> getEventSource() {
                return this.app;
            }
        };
    }

    private MySqlServer toMysqlServer(Server sqlServerInner) {
        return new MySqlServer(MySqlManagerFactory.create(ResourceId.fromString(sqlServerInner.id()).subscriptionId()), sqlServerInner);
    }

    public boolean checkNameAvailability(String name) {
        MySqlManager mySqlManager = MySqlManagerFactory.create(getDefaultSubscription().getId());
        NameAvailabilityRequest request = new NameAvailabilityRequest().withName(name).withType(NAME_AVAILABILITY_CHECK_TYPE);
        return mySqlManager.checkNameAvailabilities().execute(request).nameAvailable();
    }

    public List<Region> listSupportedRegions() {
        List<Region> locationList = az(AzureAccount.class).listRegions(getDefaultSubscription().getId());
        return locationList.stream()
            .filter(e -> MYSQL_SUPPORTED_REGIONS.contains(e.getName()))
            .distinct()
            .collect(Collectors.toList());
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
}
