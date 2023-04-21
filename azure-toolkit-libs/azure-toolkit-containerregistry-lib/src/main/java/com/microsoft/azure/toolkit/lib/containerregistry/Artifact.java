/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.containers.containerregistry.ContainerRepository;
import com.azure.containers.containerregistry.RegistryArtifact;
import com.azure.containers.containerregistry.models.ArtifactManifestProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Artifact extends AbstractAzResource<Artifact, Repository, ArtifactManifestProperties> implements Deletable {
    @Getter
    private final TagModule tagModule;
    @Nullable
    @Getter(AccessLevel.PACKAGE)
    private RegistryArtifact artifact;

    protected Artifact(@Nonnull String name, @Nonnull ArtifactModule module) {
        super(name, module);
        this.tagModule = new TagModule(this);
    }

    protected Artifact(@Nonnull Artifact artifact) {
        super(artifact);
        this.tagModule = artifact.tagModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(this.tagModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull ArtifactManifestProperties remote) {
        return Status.UNKNOWN;
    }

    @Override
    protected void updateAdditionalProperties(@Nullable ArtifactManifestProperties newRemote, @Nullable ArtifactManifestProperties oldRemote) {
        if (Objects.nonNull(newRemote)) {
            final ContainerRepository repository = Objects.requireNonNull(this.getParent().getRemote());
            this.artifact = repository.getArtifact(newRemote.getDigest());
        } else {
            this.artifact = null;
        }
    }

    @Nonnull
    public String getDigest() {
        return this.remoteOptional().map(ArtifactManifestProperties::getDigest).orElse("");
    }

    public long getSize() {
        return this.remoteOptional().map(ArtifactManifestProperties::getSizeInBytes).orElse(0L);
    }

    public List<String> getTags() {
        return this.remoteOptional().map(ArtifactManifestProperties::getTags).orElse(Collections.emptyList());
    }

    @Nonnull
    public OffsetDateTime getLastUpdateDate() {
        return this.remoteOptional().map(ArtifactManifestProperties::getLastUpdatedOn).orElse(OffsetDateTime.MIN);
    }
}
