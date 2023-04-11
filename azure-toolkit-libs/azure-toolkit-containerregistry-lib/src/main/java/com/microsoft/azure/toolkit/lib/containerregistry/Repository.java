/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.containers.containerregistry.ContainerRepository;
import com.azure.containers.containerregistry.models.ContainerRepositoryProperties;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Repository extends AbstractAzResource<Repository, ContainerRegistry, ContainerRepository> implements Deletable {
    @Getter
    private final ImageModule imageModule;
    @Nullable
    private ContainerRepositoryProperties properties;

    protected Repository(@Nonnull String name, @Nonnull RepositoryModule module) {
        super(name, module);
        this.imageModule = new ImageModule(this);
    }

    protected Repository(@Nonnull Repository registry) {
        super(registry);
        this.imageModule = registry.imageModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(imageModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull ContainerRepository remote) {
        return Status.UNKNOWN;
    }

    @Override
    protected void updateAdditionalProperties(@Nullable ContainerRepository newRemote, @Nullable ContainerRepository oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        this.properties = Optional.ofNullable(newRemote).map(ContainerRepository::getProperties).orElse(null);
    }

    @Override
    public void delete() {
        this.remoteOptional().ifPresent(ContainerRepository::delete);
    }

    public OffsetDateTime getLastUpdatedOn() {
        return Optional.ofNullable(this.properties)
            .map(ContainerRepositoryProperties::getLastUpdatedOn)
            .orElse(OffsetDateTime.MIN);
    }

    @Nonnull
    @Override
    @SneakyThrows(UnsupportedEncodingException.class)
    public String getPortalUrl() {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        final String encodedRegistryId = URLEncoder.encode(this.getParent().getId(), "UTF-8");
        final String encodedRepositoryName = URLEncoder.encode(this.getName(), "UTF-8");
        // https://ms.portal.azure.com/#blade/Microsoft_Azure_ContainerRegistries/RepositoryBlade/id/%2Fsubscriptions%2F685ba005-af8d-4b04-8f16-a7bf38b2eb5a%2FresourceGroups%2Frg-wangmi%2Fproviders%2FMicrosoft.ContainerRegistry%2Fregistries%2Fwangmistandard/repository/gr8miller%2Fcontainerapps-albumapi-javascript
        return String.format("%s/#blade/Microsoft_Azure_ContainerRegistries/RepositoryBlade/id/%s/repository/%s", account.getPortalUrl(), encodedRegistryId, encodedRepositoryName);
    }

    public String getFullName() {
        final ContainerRegistry registry = this.getParent();
        return String.format("%s/%s", registry.getLoginServerUrl(), this.getName());
    }
}
