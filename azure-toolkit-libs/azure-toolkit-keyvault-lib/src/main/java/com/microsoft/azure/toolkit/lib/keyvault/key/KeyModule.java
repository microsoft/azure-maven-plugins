/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.key;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.keys.models.KeyProperties;
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

public class KeyModule extends AbstractAzResourceModule<Key, KeyVault, KeyProperties> {
    public static final String NAME = "keys";

    public KeyModule(KeyVault parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, KeyProperties>> loadResourcePagesFromAzure() {
        try {
            return Optional.ofNullable(getClient())
                .map(c -> c.listPropertiesOfKeys().toStream().filter(p -> BooleanUtils.isNotTrue(p.isManaged())).collect(Collectors.toList()))
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
    @AzureOperation(name = "azure/keyvault.load_key.key", params = {"name"})
    protected KeyProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        try {
            final List<KeyProperties> versions = Optional.ofNullable(getClient())
                .map(keys -> keys.listPropertiesOfKeys().collectList().block())
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
    @AzureOperation(name = "azure/keyvault.delete_key.key", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(this.getClient())
            .ifPresent(client -> client.beginDeleteKey(id.name()).blockLast());
    }

    @Nonnull
    @Override
    protected Key newResource(@Nonnull KeyProperties remote) {
        return new Key(remote, this);
    }

    @Nonnull
    @Override
    protected Key newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Key(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected KeyDraft newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new KeyDraft(name, Objects.requireNonNull(rgName), this);
    }

    @Nullable
    @Override
    protected KeyAsyncClient getClient() {
        return this.getParent().getKeyClient();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Key";
    }
}
