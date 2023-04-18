/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.containers.containerregistry.ContainerRepository;
import com.azure.containers.containerregistry.models.ArtifactManifestProperties;
import com.azure.core.util.paging.ContinuablePage;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class ArtifactModule extends AbstractAzResourceModule<Artifact, Repository, ArtifactManifestProperties> {

    public static final String NAME = "artifacts";

    public ArtifactModule(@Nonnull Repository parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, ArtifactManifestProperties>> loadResourcePagesFromAzure() {
        if (!this.parent.exists()) {
            return Collections.emptyIterator();
        }
        return Optional.ofNullable(this.parent.getRemote())
            .map(r -> r.listManifestProperties().streamByPage(getPageSize()).iterator())
            .orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    protected ArtifactManifestProperties loadResourceFromAzure(@Nonnull String name, String unused) {
        if (!this.parent.exists()) {
            return null;
        }
        final ContainerRepository remote = Objects.requireNonNull(this.parent.getRemote());
        return remote.listManifestProperties().stream()
            .filter(p -> p.getRepositoryName().equalsIgnoreCase(name))
            .findAny().orElse(null);
    }

    @Nonnull
    @Override
    protected Artifact newResource(@Nonnull ArtifactManifestProperties manifest) {
        return new Artifact(manifest.getRepositoryName(), this);
    }

    @Nonnull
    @Override
    protected Artifact newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Artifact(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Artifact";
    }
}
