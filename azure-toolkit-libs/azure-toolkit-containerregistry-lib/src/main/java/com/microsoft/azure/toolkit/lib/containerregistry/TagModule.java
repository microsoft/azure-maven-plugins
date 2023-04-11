/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.containers.containerregistry.RegistryArtifact;
import com.azure.containers.containerregistry.models.ArtifactManifestProperties;
import com.azure.containers.containerregistry.models.ArtifactTagProperties;
import com.azure.core.util.paging.ContinuablePage;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

public class TagModule extends AbstractAzResourceModule<Tag, Image, ArtifactTagProperties> {

    public static final String NAME = "tags";

    public TagModule(@Nonnull Image parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, ArtifactTagProperties>> loadResourcePagesFromAzure() {
        if (!this.parent.exists()) {
            return Collections.emptyIterator();
        }
        final ArtifactManifestProperties remote = Objects.requireNonNull(this.parent.getRemote());
        final RegistryArtifact artifact = Objects.requireNonNull(this.parent.getParent().getRemote()).getArtifact(this.getParent().getDigest());
        return Collections.singletonList(new ItemPage<>(remote.getTags().stream().map(artifact::getTagProperties))).iterator();
    }

    @Nullable
    @Override
    protected ArtifactTagProperties loadResourceFromAzure(@Nonnull String name, String unused) {
        if (!this.parent.exists()) {
            return null;
        }
        final RegistryArtifact artifact = Objects.requireNonNull(this.parent.getParent().getRemote()).getArtifact(this.getParent().getDigest());
        return artifact.getTagProperties(name);
    }

    @Nonnull
    @Override
    protected Tag newResource(@Nonnull ArtifactTagProperties tag) {
        return new Tag(tag.getName(), this);
    }

    @Nonnull
    @Override
    protected Tag newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Tag(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Tag";
    }
}
