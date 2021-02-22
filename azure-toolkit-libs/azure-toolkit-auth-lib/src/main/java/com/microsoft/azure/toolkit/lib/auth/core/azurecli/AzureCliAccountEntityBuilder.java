/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.azurecli;

import com.azure.core.credential.TokenCredential;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.SubscriptionEntity;
import com.microsoft.azure.toolkit.lib.auth.util.AzCommandUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureCliAccountEntityBuilder implements IAccountEntityBuilder {
    public AccountEntity build() {
        AccountEntity profile = new AccountEntity();
        profile.setMethod(AuthMethod.AZURE_CLI);
        profile.setAuthenticated(false);

        String cliVersion = getVersion();
        if (StringUtils.isBlank(cliVersion)) {
            return profile;
        }

        final List<SubscriptionEntity> subscriptions = listSubscriptions();
        if (CollectionUtils.isEmpty(subscriptions)) {
            return profile;
        }

        SubscriptionEntity defaultSubscription = subscriptions.stream().filter(SubscriptionEntity::isSelected).findFirst().orElse(subscriptions.get(0));
        profile.setTenantIds(subscriptions.stream().map(SubscriptionEntity::getTenantId).distinct().collect(Collectors.toList()));
        List<SubscriptionEntity> selectedSubscriptions = subscriptions.stream()
                .filter(SubscriptionEntity::isSelected).collect(Collectors.toList());
        profile.setSelectedSubscriptionIds(selectedSubscriptions.stream().map(SubscriptionEntity::getId).collect(Collectors.toList()));
        profile.setEmail(defaultSubscription.getEmail());
        profile.setEnvironment(defaultSubscription.getEnvironment());

        profile.setCredentialBuilder(new ICredentialBuilder() {
            @Override
            public TokenCredential getCredentialWrapperForSubscription(SubscriptionEntity subscriptionEntity) {
                Objects.requireNonNull(subscriptionEntity, "Parameter 'subscriptionEntity' cannot be null for building credentials.");
                return new AzureCliTenantCredential(subscriptionEntity.getTenantId());
            }

            @Override
            public TokenCredential getCredentialForTenant(String tenantId) {
                return new AzureCliTenantCredential(tenantId);
            }

            @Override
            public TokenCredential getCredentialForListingTenants() {
                return new AzureCliTenantCredential();
            }
        });
        profile.setAuthenticated(true);
        return profile;
    }

    private static String getVersion() {
        try {
            final JsonObject result = AzCommandUtils.executeAzCommandJson("az version --output json").getAsJsonObject();
            return result.get("azure-cli").getAsString();
        } catch (NullPointerException ex) {
        }
        return null;
    }

    private static List<SubscriptionEntity> listSubscriptions() {
        final JsonArray result = AzCommandUtils.executeAzCommandJson("az account list --output json").getAsJsonArray();
        final List<SubscriptionEntity> list = new ArrayList<>();
        if (result != null) {
            result.forEach(j -> {
                JsonObject accountObject = j.getAsJsonObject();
                if (!accountObject.has("id")) {
                    return;
                }
                // TODO: use utility to handle the json mapping
                String tenantId = accountObject.get("tenantId").getAsString();
                String subscriptionId = accountObject.get("id").getAsString();
                String subscriptionName = accountObject.get("name").getAsString();
                String state = accountObject.get("state").getAsString();
                String cloud = accountObject.get("cloudName").getAsString();
                String email = accountObject.get("user").getAsJsonObject().get("name").getAsString();

                if (StringUtils.equals(state, "Enabled") && StringUtils.isNoneBlank(subscriptionId, subscriptionName)) {
                    SubscriptionEntity entity = new SubscriptionEntity();
                    entity.setId(subscriptionId);
                    entity.setName(subscriptionName);
                    entity.setSelected(accountObject.get("isDefault").getAsBoolean());
                    entity.setTenantId(tenantId);
                    entity.setEmail(email);
                    entity.setEnvironment(cloud);
                    list.add(entity);
                }
            });
            return list;
        }
        return null;
    }
}
