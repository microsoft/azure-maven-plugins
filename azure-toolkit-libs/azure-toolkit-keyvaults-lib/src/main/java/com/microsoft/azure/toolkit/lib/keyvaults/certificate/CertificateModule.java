/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.certificate;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.core.util.polling.SyncPoller;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.security.keyvault.certificates.CertificateClient;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.certificates.models.DeletedCertificate;
import com.azure.security.keyvault.certificates.models.KeyVaultCertificate;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class CertificateModule extends AbstractAzResourceModule<Certificate, KeyVault, CertificateProperties> {
    public static final String NAME = "certificates";

    public CertificateModule(KeyVault parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, CertificateProperties>> loadResourcePagesFromAzure() {
        final CertificateClient client = getClient();
        return Optional.ofNullable(client)
            .map(c -> c.listPropertiesOfCertificates().iterableByPage(getPageSize()).iterator())
            .orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvaults.load_key_vault.key_vault", params = {"name"})
    protected CertificateProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(this.getClient())
            .map(client -> client.getCertificate(name))
            .map(KeyVaultCertificate::getProperties)
            .orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/keyvaults.delete_key_vault.key_vault", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final CertificateClient client = getClient();
        if (Objects.isNull(client)) {
            return;
        }
        final ResourceId id = ResourceId.fromString(resourceId);
        final SyncPoller<DeletedCertificate, Void> deletedCertificateVoidSyncPoller = client.beginDeleteCertificate(id.name());
        deletedCertificateVoidSyncPoller.waitForCompletion();
    }

    @Nonnull
    @Override
    protected Certificate newResource(@Nonnull CertificateProperties remote) {
        return new Certificate(remote, this);
    }

    @Nonnull
    @Override
    protected Certificate newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Certificate(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nullable
    @Override
    protected CertificateClient getClient() {
        return getParent().getCertificateClient();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Certificate";
    }
}
