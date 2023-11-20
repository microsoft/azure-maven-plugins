/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.certificate;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.certificates.models.KeyVaultCertificate;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.collections4.IteratorUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class CertificateVersionModule extends AbstractAzResourceModule<CertificateVersion, Certificate, CertificateProperties> {
    public static final String NAME = "versions";

    public CertificateVersionModule(Certificate parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, CertificateProperties>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(getClient())
            .map(keys -> keys.listPropertiesOfCertificateVersions(getParent().getName()).collectList().block())
            .map(ItemPage::new)
            .map(IteratorUtils::singletonIterator)
            .orElseGet(IteratorUtils::emptyIterator);
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvaults.load_key_vault.key_vault", params = {"name"})
    protected CertificateProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient())
            .map(v -> v.getCertificateVersion(getParent().getName(), name).block())
            .map(KeyVaultCertificate::getProperties)
            .orElse(null);
    }

    @Nonnull
    @Override
    protected CertificateVersion newResource(@Nonnull CertificateProperties remote) {
        return new CertificateVersion(remote, this);
    }

    @Nonnull
    @Override
    protected CertificateVersion newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new CertificateVersion(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected CertificateVersionDraft newDraftForUpdate(@Nonnull CertificateVersion version) {
        return new CertificateVersionDraft(version);
    }

    @Nullable
    @Override
    protected CertificateAsyncClient getClient() {
        return this.getParent().getParent().getCertificateClient();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Certificate Version";
    }
}

