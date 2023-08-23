package com.microsoft.azure.toolkit.lib.cognitiveservices;

import com.azure.resourcemanager.cognitiveservices.models.Account;
import com.azure.resourcemanager.cognitiveservices.models.AccountProperties;
import com.azure.resourcemanager.cognitiveservices.models.Accounts;
import com.azure.resourcemanager.cognitiveservices.models.PublicNetworkAccess;
import com.azure.resourcemanager.cognitiveservices.models.Sku;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountSku;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class CognitiveAccountDraft extends CognitiveAccount
    implements AzResource.Draft<CognitiveAccount, Account> {
    public static final String OPEN_AI = "OpenAI";

    @Getter
    @Nullable
    private final CognitiveAccount origin;

    @Getter
    @Setter
    private CognitiveAccountDraft.Config config;

    protected CognitiveAccountDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CognitiveAccountModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    protected CognitiveAccountDraft(@Nonnull CognitiveAccount origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/openai.create_account.account", params = {"this.getName()"})
    public Account createResourceInAzure() {
        final Accounts client = Objects.requireNonNull(((CognitiveAccountModule) getModule()).getClient());
        final Region region = Objects.requireNonNull(getRegion(), "'region' is required to create cognitive account.");
        final AccountSku sku = Objects.requireNonNull(getSku(), "'sku' is required to create cognitive account.");
        final ResourceGroup resourceGroup = Objects.requireNonNull(getResourceGroup());
        if (resourceGroup.isDraftForCreating()) {
            ((ResourceGroupDraft) resourceGroup).setRegion(getRegion());
            ((ResourceGroupDraft) resourceGroup).createIfNotExist();
        }
        final AccountProperties properties = new AccountProperties();
        // may support set access limit later
        properties.withPublicNetworkAccess(PublicNetworkAccess.ENABLED);
        AzureMessager.getMessager().info(AzureString.format("Start creating Cognitive account({0})...", this.getName()));
        final Account result = client.define(this.getName())
            .withExistingResourceGroup(resourceGroup.getName())
            .withKind(OPEN_AI)
            .withRegion(region.getName())
            .withSku(new Sku().withName(sku.getName()))
            .withProperties(properties)
            .create();
        final Action<CognitiveAccount> create = AzureActionManager.getInstance().getAction(CREATE_DEPLOYMENT).bind(this);
        AzureMessager.getMessager().success(AzureString.format("Cognitive account({0}) is successfully created.", this.getName()), create);
        return result;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/openai.update_account.account", params = {"this.getName()"})
    public Account updateResourceInAzure(@Nonnull Account origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }


    @Override
    public boolean isModified() {
        return Objects.nonNull(config) && Objects.equals(config, new CognitiveAccountDraft.Config());
    }

    @Nullable
    @Override
    public AccountSku getSku() {
        return Optional.ofNullable(config).map(Config::getSku).orElse(null);
    }

    @Nullable
    @Override
    public Region getRegion() {
        return Optional.ofNullable(config).map(Config::getRegion).orElse(null);
    }

    @Nullable
    @Override
    public ResourceGroup getResourceGroup() {
        return Optional.ofNullable(config).map(Config::getResourceGroup).orElseGet(super::getResourceGroup);
    }

    @Data
    @Builder
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private String name;
        private ResourceGroup resourceGroup;
        private Region region;
        private AccountSku sku;
    }
}
