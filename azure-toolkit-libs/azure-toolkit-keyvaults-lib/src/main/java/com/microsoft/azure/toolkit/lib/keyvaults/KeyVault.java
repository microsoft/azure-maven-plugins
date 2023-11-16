/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults;

import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.CertificateClientBuilder;
import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.keyvaults.certificate.CertificateModule;
import com.microsoft.azure.toolkit.lib.keyvaults.key.KeyModule;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.SecretModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class KeyVault extends AbstractAzResource<KeyVault, KeyVaultSubscription, Vault> implements Deletable {

    private final KeyModule keyModule;
    private final SecretModule secretModule;
    private final CertificateModule certificateModule;

    @Nullable
    private volatile CertificateAsyncClient certificateClient;

    protected KeyVault(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull KeyVaultModule module) {
        super(name, resourceGroupName, module);
        this.keyModule = new KeyModule(this);
        this.secretModule = new SecretModule(this);
        this.certificateModule = new CertificateModule(this);
    }

    protected KeyVault(@Nonnull KeyVault origin) {
        super(origin);
        this.keyModule = origin.keyModule;
        this.secretModule = origin.secretModule;
        this.certificateModule = origin.certificateModule;

        this.certificateClient = origin.certificateClient;
    }

    protected KeyVault(@Nonnull Vault remote, @Nonnull KeyVaultModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.keyModule = new KeyModule(this);
        this.secretModule = new SecretModule(this);
        this.certificateModule = new CertificateModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Arrays.asList(keyModule, secretModule, certificateModule);
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull Vault remote) {
        return remote.innerModel().properties().provisioningState().toString();
    }

    @Nullable
    public String getVaultUri() {
        return Optional.ofNullable(getRemote()).map(Vault::vaultUri).orElse(null);
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(Vault::regionName).map(Region::fromName).orElse(null);
    }

    public KeyModule keys() {
        return this.keyModule;
    }

    public SecretModule secrets() {
        return this.secretModule;
    }

    public CertificateModule certificates() {
        return this.certificateModule;
    }

    public CertificateAsyncClient getCertificateClient() {
        if (certificateClient == null) {
            synchronized (this) {
                if (certificateClient == null && Objects.nonNull(getVaultUri())) {
                    final Account account = Azure.az(AzureAccount.class).account();
                    certificateClient = new CertificateClientBuilder()
                        .vaultUrl(getVaultUri())
                        .credential(account.getTokenCredential(getSubscriptionId()))
                        .buildAsyncClient();
                }
            }
        }
        return certificateClient;
    }

    public SecretAsyncClient getSecretClient() {
        return Optional.ofNullable(getRemote()).map(Vault::secretClient).orElse(null);
    }

    public KeyAsyncClient getKeyClient() {
        return Optional.ofNullable(getRemote()).map(Vault::keyClient).orElse(null);
    }
}
