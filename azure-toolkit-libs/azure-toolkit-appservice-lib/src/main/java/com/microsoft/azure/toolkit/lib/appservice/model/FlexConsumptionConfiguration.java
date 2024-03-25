/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.resources.fluentcore.arm.models.HasResourceGroup;
import com.azure.resourcemanager.storage.StorageManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class FlexConsumptionConfiguration {
    public static final int DEFAULT_INSTANCE_SIZE = 2048;
    public static final FlexConsumptionConfiguration DEFAULT_CONFIGURATION = FlexConsumptionConfiguration.builder()
        .authenticationMethod(StorageAuthenticationMethod.StorageAccountConnectionString)
        .storageAccountConnectionString(FunctionAppConfig.Storage.Authentication.DEPLOYMENT_STORAGE_CONNECTION_STRING)
        .instanceSize(DEFAULT_INSTANCE_SIZE)
        .maximumInstances(FunctionAppConfig.FunctionScaleAndConcurrency.DEFAULT_MAXIMUM_INSTANCE_COUNT)
        .build();

    // deployment
    private String deploymentResourceGroup;
    private String deploymentAccount;
    private String deploymentContainer;
    // deployment - authentication
    private StorageAuthenticationMethod authenticationMethod;
    private String userAssignedIdentityResourceId;
    private String storageAccountConnectionString;
    // scale configuration
    private Integer instanceSize;
    private Map<String, String> alwaysReadyInstances;
    private Integer maximumInstances;

    private FunctionAppConfig flexFunctionAppConfig;

    public boolean isEmpty() {
        return ObjectUtils.allNull(instanceSize, alwaysReadyInstances, maximumInstances);
    }

    public FunctionAppConfig.FunctionAlwaysReadyConfig[] getAlwaysReadyInstances() {
        if (MapUtils.isEmpty(alwaysReadyInstances)) {
            return null;
        }
        return alwaysReadyInstances.entrySet().stream()
            .map(entry -> FunctionAppConfig.FunctionAlwaysReadyConfig.builder()
                .name(entry.getKey()).instanceCount(Integer.valueOf(entry.getValue())).build())
            .toArray(FunctionAppConfig.FunctionAlwaysReadyConfig[]::new);
    }

    @Nullable
    public static FlexConsumptionConfiguration fromFunctionAppBase(@Nonnull final FunctionAppBase<?, ?, ?> app) {
        final FunctionAppConfig config = app.getFlexConsumptionAppConfig();
        if (Objects.isNull(config)) {
            return null;
        }
        final FlexConsumptionConfigurationBuilder builder = FlexConsumptionConfiguration.builder();
        // deployment part
        final FunctionAppConfig.Storage storage = config.getDeployment().getStorage();
        Optional.ofNullable(storage.getValue()).ifPresent(value -> {
            builder.deploymentAccount(parseStorageAccountFromBlobUrl(value));
            builder.deploymentContainer(parseContainerFromBlobUrl(value));
            builder.deploymentResourceGroup(getStorageAccountResourceGroup(app, builder.deploymentAccount));
        });
        Optional.ofNullable(storage.getAuthentication()).ifPresent(auth -> {
            builder.authenticationMethod(auth.getType());
            builder.userAssignedIdentityResourceId(auth.getUserAssignedIdentityResourceId());
            builder.storageAccountConnectionString(auth.getStorageAccountConnectionStringName());
        });
        // scale part
        final FunctionAppConfig.FunctionScaleAndConcurrency scale = config.getScaleAndConcurrency();
        builder.instanceSize(scale.getInstanceMemoryMB());
        builder.maximumInstances(scale.getMaximumInstanceCount());
        final Map<String, String> alwaysReady = new HashMap<>();
        if (ArrayUtils.isNotEmpty(scale.getAlwaysReady())) {
            Arrays.stream(scale.getAlwaysReady()).forEach(c -> alwaysReady.put(c.getName(), String.valueOf(c.getInstanceCount())));
        }
        builder.alwaysReadyInstances(alwaysReady);
        return builder.build();
    }

    private static String getStorageAccountResourceGroup(@Nonnull final FunctionAppBase<?, ?, ?> app, final String deploymentAccount) {
        final StorageManager remote = Azure.az(AzureStorageAccount.class).forSubscription(app.getSubscriptionId()).getRemote();
        // call sdk directly as Azure.az(AzureStorageAccount.class).list() has poor perf
        // it's okay to compare name only as name for storage account is unique
        return Objects.requireNonNull(remote).storageAccounts().list().stream().collect(Collectors.toList())
            .stream().filter(account -> account.name().equals(deploymentAccount))
            .findFirst()
            .map(HasResourceGroup::resourceGroupName)
            .orElse(app.getResourceGroupName());
    }

    private static String parseStorageAccountFromBlobUrl(@Nonnull String blobUrl) {
        // https://<storageAccountName>.blob.core.windows.net/<containerName>
        final String[] parts = blobUrl.split("/", 4);
        if (parts.length < 3) {
            return null;
        }
        return parts[2].split("\\.")[0];
    }

    private static String parseContainerFromBlobUrl(@Nonnull String blobUrl) {
        final String[] parts = blobUrl.split("/", 4);
        if (parts.length < 4) {
            return null;
        }
        return parts[3];
    }
}
