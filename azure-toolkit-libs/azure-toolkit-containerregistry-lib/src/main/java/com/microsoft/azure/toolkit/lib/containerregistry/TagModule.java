/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.containers.containerregistry.RegistryArtifact;
import com.azure.containers.containerregistry.models.ArtifactTagProperties;
import com.azure.core.util.paging.ContinuablePage;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TagModule extends AbstractAzResourceModule<Tag, Artifact, ArtifactTagProperties> {

    public static final String NAME = "tags";

    public TagModule(@Nonnull Artifact parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, ArtifactTagProperties>> loadResourcePagesFromAzure() {
        final List<String> tags = Optional.of(this.getParent())
            .filter(AbstractAzResource::exists)
            .map(Artifact::getTags)
            .orElse(Collections.emptyList());
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyIterator();
        }
        final Repository repository = this.getParent().getParent();
        final RegistryArtifact image = Objects.requireNonNull(repository.getRemote()).getArtifact(this.getParent().getDigest());
        return Collections.singletonList(new ItemPage<>(tags.stream().map(image::getTagProperties))).iterator();
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

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final Tag tag = this.get(resourceId);
        Optional.ofNullable(tag)
            .map(AbstractAzResource::getParent)
            .map(Artifact::getArtifact)
            .ifPresent(a -> a.deleteTag(tag.getName()));
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
