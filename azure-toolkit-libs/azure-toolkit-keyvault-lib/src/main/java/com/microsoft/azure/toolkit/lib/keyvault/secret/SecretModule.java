/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.secret;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource.isHttpException;
import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyConfiureAction;
import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyLearnMoreAction;

public class SecretModule extends AbstractAzResourceModule<Secret, KeyVault, SecretProperties> {
    public static final String NAME = "secrets";

    public SecretModule(KeyVault parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, SecretProperties>> loadResourcePagesFromAzure() {
        try {
            return Optional.ofNullable(getClient())
                .map(c -> c.listPropertiesOfSecrets().toStream().filter(p -> BooleanUtils.isNotTrue(p.isManaged())).collect(Collectors.toList()))
                .map(ItemPage::new)
                .map(IteratorUtils::singletonIterator)
                .orElseGet(IteratorUtils::emptyIterator);
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                final Action<String> configure = getAccessPolicyConfiureAction(getParent());
                final Action<String> learnMore = getAccessPolicyLearnMoreAction();
                throw new AzureToolkitRuntimeException(ExceptionUtils.getRootCauseMessage(t), configure, learnMore);
            }
            throw t;
        }
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvault.load_secret.secret", params = {"name"})
    protected SecretProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        try {
            final List<SecretProperties> versions = Optional.ofNullable(getClient())
                .map(keys -> keys.listPropertiesOfSecrets().collectList().block())
                .orElse(null);
            return Objects.isNull(versions) ? null : versions.stream()
                .filter(s -> StringUtils.equalsIgnoreCase(s.getName(), name))
                .findFirst()
                .orElse(null);
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                final Action<String> configure = getAccessPolicyConfiureAction(getParent());
                final Action<String> learnMore = getAccessPolicyLearnMoreAction();
                throw new AzureToolkitRuntimeException(ExceptionUtils.getRootCauseMessage(t), configure, learnMore);
            }
            throw t;
        }
    }

    @Override
    @AzureOperation(name = "azure/keyvault.delete_secret.secret", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(this.getClient()).ifPresent(vaults -> vaults.beginDeleteSecret(id.name()).blockLast());
    }

    @Nonnull
    @Override
    protected Secret newResource(@Nonnull SecretProperties remote) {
        return new Secret(remote, this);
    }

    @Nonnull
    @Override
    protected Secret newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Secret(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nullable
    @Override
    protected SecretAsyncClient getClient() {
        return getParent().getSecretClient();
    }

    @Nonnull
    @Override
    protected SecretDraft newDraftForCreate(@Nonnull String name, @org.jetbrains.annotations.Nullable String rgName) {
        return new SecretDraft(name, rgName, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Secret";
    }
}

