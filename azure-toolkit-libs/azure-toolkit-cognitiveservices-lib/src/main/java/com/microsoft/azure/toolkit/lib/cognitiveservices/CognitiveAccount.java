/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.resourcemanager.cognitiveservices.models.Account;
import com.azure.resourcemanager.cognitiveservices.models.AccountProperties;
import com.azure.resourcemanager.cognitiveservices.models.Accounts;
import com.azure.resourcemanager.cognitiveservices.models.ApiKeys;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountSku;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.azure.resourcemanager.cognitiveservices.models.ModelLifecycleStatus.GENERALLY_AVAILABLE;

public class CognitiveAccount extends AbstractAzResource<CognitiveAccount, CognitiveServicesSubscription, Account>
    implements Deletable {
    public static final Action.Id<CognitiveAccount> CREATE_DEPLOYMENT = Action.Id.of("user/openai.create_deployment.account");

    private final CognitiveDeploymentModule deploymentModule;

    protected CognitiveAccount(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CognitiveAccountModule module) {
        super(name, resourceGroupName, module);
        this.deploymentModule = new CognitiveDeploymentModule(this);
    }

    protected CognitiveAccount(@Nonnull CognitiveAccount insight) {
        super(insight);
        this.deploymentModule = insight.deploymentModule;
    }

    protected CognitiveAccount(@Nonnull Account remote, @Nonnull CognitiveAccountModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.deploymentModule = new CognitiveDeploymentModule(this);
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull Account remote) {
        return remote.properties().provisioningState().toString();
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Arrays.asList(this.deploymentModule);
    }

    @Nullable
    public String getPrimaryKey() {
        return Optional.ofNullable(getRemote()).map(Account::listKeys)
            .map(ApiKeys::key1).orElse(null);
    }

    @Nullable
    public String getSecondaryKey() {
        return Optional.ofNullable(getRemote()).map(Account::listKeys)
            .map(ApiKeys::key2).orElse(null);
    }

    @Nonnull
    public List<AccountModel> listModels() {
        final Accounts accounts = ((CognitiveAccountModule) getModule()).getClient();
        if (Objects.isNull(accounts)) {
            return Collections.emptyList();
        }
        return accounts.listModels(this.getResourceGroupName(), this.getName()).stream()
            .filter(model -> CollectionUtils.isNotEmpty(model.skus()) && model.lifecycleStatus() == GENERALLY_AVAILABLE)
            .map(AccountModel::fromModel)
            .collect(Collectors.toList());
    }

    @Nullable
    public String getEndpoint() {
        return Optional.ofNullable(getRemote()).map(Account::properties).map(AccountProperties::endpoint).orElse(null);
    }

    @Nonnull
    public OpenAIClient getOpenAIClient() {
        return new OpenAIClientBuilder()
            .httpLogOptions(new HttpLogOptions().setLogLevel(Azure.az().config().getLogLevel()))
            .addPolicy(Azure.az().config().getUserAgentPolicy())
            .credential(new AzureKeyCredential(Objects.requireNonNull(this.getPrimaryKey(), String.format("Failed to get primary key for account %s", getName()))))
            .endpoint(getEndpoint())
            .buildClient();
    }

    @Nullable
    public AccountSku getSku() {
        return Optional.ofNullable(getRemote())
            .map(Account::sku)
            .map(AccountSku::fromSku).orElse(null);
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(Account::location).map(Region::fromName).orElse(null);
    }

    @Nonnull
    public CognitiveDeploymentModule deployments() {
        return this.deploymentModule;
    }
}
