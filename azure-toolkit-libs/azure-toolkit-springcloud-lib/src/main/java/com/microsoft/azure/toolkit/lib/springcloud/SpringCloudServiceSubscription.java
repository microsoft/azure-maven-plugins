/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.management.Region;
import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.appplatform.models.NameAvailability;
import com.azure.resourcemanager.appplatform.models.NameAvailabilityParameters;
import com.azure.resourcemanager.appplatform.models.ResourceSku;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.SupportedRuntimePlatform;
import com.azure.resourcemanager.appplatform.models.SupportedRuntimeVersion;
import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.model.Sku;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class SpringCloudServiceSubscription extends AbstractAzServiceSubscription<SpringCloudServiceSubscription, AppPlatformManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final SpringCloudClusterModule clusterModule;

    SpringCloudServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureSpringCloud service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.clusterModule = new SpringCloudClusterModule(this);
    }

    SpringCloudServiceSubscription(@Nonnull AppPlatformManager remote, @Nonnull AzureSpringCloud service) {
        this(remote.subscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(clusterModule);
    }

    @Nonnull
    public SpringCloudClusterModule clusters() {
        return this.clusterModule;
    }

    @Nonnull
    public List<RuntimeVersion> listSupportedRuntimeVersions() {
        return Objects.requireNonNull(this.getRemote()).serviceClient()
            .getRuntimeVersions().listRuntimeVersions().value().stream()
            .filter(v -> v.platform() == SupportedRuntimePlatform.JAVA)
            .map(SupportedRuntimeVersion::value)
            .map(v -> RuntimeVersion.fromString(v.toString())).collect(Collectors.toList());
    }

    @Nonnull
    public List<Sku> listSupportedSkus(@Nullable Region region) {
        Stream<ResourceSku> skus = Objects.requireNonNull(this.getRemote()).serviceClient().getSkus().list().stream();
        if (Objects.nonNull(region)) {
            skus = skus.filter(s -> s.locations().contains(region.name()));
        }
        return skus
            .filter(s -> s.resourceType().equalsIgnoreCase("Spring"))
            .map(sku -> new Sku(sku.name(), sku.tier()))
            .distinct()
            .sorted(Comparator.comparing(Sku::getOrdinal))
            .collect(Collectors.toList());
    }

    @Nonnull
    public List<Region> listSupportedRegions(@Nullable Sku sku) {
        Stream<ResourceSku> skus = Objects.requireNonNull(this.getRemote()).serviceClient().getSkus().list().stream();
        if (Objects.nonNull(sku)) {
            skus = skus.filter(s -> StringUtils.equalsIgnoreCase(sku.getName(), s.name()) && StringUtils.equalsIgnoreCase(sku.getTier(), s.tier()));
        }
        return skus.flatMap(s -> s.locations().stream().map(Region::fromName))
            .filter(Utils.distinctByKey(Region::name))
            .sorted(Comparator.comparing(Region::label))
            .collect(Collectors.toList());
    }

    @Nonnull
    @AzureOperation(name = "azure/springcloud.check_name.name", params = "name")
    public NameAvailability checkNameAvailability(Region region, String name) {
        return Objects.requireNonNull(this.getRemote()).serviceClient().getServices().checkNameAvailability(region.name(), new NameAvailabilityParameters()
            .withName(name).withType(this.clusterModule.getFullResourceType()));
    }

    @Nonnull
    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }
}

