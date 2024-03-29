/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.key;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource.isHttpException;
import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyConfiureAction;
import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyLearnMoreAction;

public class KeyVersionModule extends AbstractAzResourceModule<KeyVersion, Key, KeyProperties> {
    public static final String NAME = "versions";

    public KeyVersionModule(Key parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, KeyProperties>> loadResourcePagesFromAzure() {
        try {
            return Optional.ofNullable(getClient())
                .map(keys -> keys.listPropertiesOfKeyVersions(getParent().getName()).collectList().block())
                .map(ItemPage::new)
                .map(IteratorUtils::singletonIterator)
                .orElseGet(IteratorUtils::emptyIterator);
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                final Action<String> configure = getAccessPolicyConfiureAction(getParent().getKeyVault());
                final Action<String> learnMore = getAccessPolicyLearnMoreAction();
                throw new AzureToolkitRuntimeException(ExceptionUtils.getRootCauseMessage(t), configure, learnMore);
            }
            throw t;
        }
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvault.load_key_version.version", params = {"name"})
    protected KeyProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        try {
            final List<KeyProperties> versions = Optional.ofNullable(getClient())
                .map(keys -> keys.listPropertiesOfKeyVersions(getParent().getName()).collectList().block())
                .orElse(null);
            return Objects.isNull(versions) ? null : versions.stream()
                .filter(s -> StringUtils.equalsIgnoreCase(s.getVersion(), name))
                .findFirst()
                .orElse(null);
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                final Action<String> configure = getAccessPolicyConfiureAction(getParent().getKeyVault());
                final Action<String> learnMore = getAccessPolicyLearnMoreAction();
                throw new AzureToolkitRuntimeException(ExceptionUtils.getRootCauseMessage(t), configure, learnMore);
            }
            throw t;
        }
    }

    @Nonnull
    @Override
    protected KeyVersion newResource(@Nonnull KeyProperties remote) {
        return new KeyVersion(remote, this);
    }

    @Nonnull
    @Override
    protected KeyVersion newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new KeyVersion(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<KeyVersion, KeyProperties> newDraftForUpdate(@Nonnull KeyVersion version) {
        return new KeyVersionDraft(version);
    }

    @Nullable
    @Override
    protected KeyAsyncClient getClient() {
        return this.getParent().getParent().getKeyClient();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Key Version";
    }
}

