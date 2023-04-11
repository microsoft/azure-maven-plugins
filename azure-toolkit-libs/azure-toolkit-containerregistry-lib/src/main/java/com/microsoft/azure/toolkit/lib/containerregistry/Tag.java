/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.containers.containerregistry.models.ArtifactTagProperties;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Tag extends AbstractAzResource<Tag, Image, ArtifactTagProperties> implements Deletable {
    protected Tag(@Nonnull String name, @Nonnull TagModule module) {
        super(name, module);
    }

    protected Tag(@Nonnull Tag tag) {
        super(tag);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull ArtifactTagProperties remote) {
        return Status.UNKNOWN;
    }

    @Override
    public void delete() {
        Optional.ofNullable(this.getParent().getArtifact()).ifPresent(a -> a.deleteTag(this.getName()));
    }

    @Nonnull
    @Override
    @SneakyThrows(UnsupportedEncodingException.class)
    public String getPortalUrl() {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        final Repository repository = this.getParent().getParent();
        final ContainerRegistry registry = repository.getParent();
        final String encodedRegistryId = URLEncoder.encode(registry.getId(), "UTF-8");
        final String encodedRepositoryName = URLEncoder.encode(repository.getName(), "UTF-8");
        // https://ms.portal.azure.com/#blade/Microsoft_Azure_ContainerRegistries/TagMetadataBlade/registryId/%2Fsubscriptions%2F685ba005-af8d-4b04-8f16-a7bf38b2eb5a%2FresourceGroups%2Frg-wangmi%2Fproviders%2FMicrosoft.ContainerRegistry%2Fregistries%2Fwangmistandard/repositoryName/gr8miller%2Fcontainerapps-albumapi-javascript/tag/1.0.0
        return String.format("%s/#blade/Microsoft_Azure_ContainerRegistries/TagMetadataBlade/registryId/%s/repositoryName/%s/tag/%s", account.getPortalUrl(), encodedRegistryId, encodedRepositoryName, this.getName());
    }

    @Nonnull
    public String getDigest() {
        return this.remoteOptional().map(ArtifactTagProperties::getDigest).orElse("");
    }

    @Nonnull
    public OffsetDateTime getLastUpdatedOn() {
        return this.remoteOptional().map(ArtifactTagProperties::getLastUpdatedOn).orElse(OffsetDateTime.MIN);
    }

    public String getFullName() {
        final Repository repository = this.getParent().getParent();
        final ContainerRegistry registry = repository.getParent();
        return String.format("%s/%s:%s", registry.getLoginServerUrl(), repository.getName(), this.getName());
    }
}
