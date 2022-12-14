/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.containers.containerregistry.ContainerRepository;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Repository extends AbstractAzResource<Repository, ContainerRegistry, ContainerRepository> {
    protected Repository(@Nonnull String name, @Nonnull RepositoryModule module) {
        super(name, module);
    }

    protected Repository(@Nonnull Repository registry) {
        super(registry);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull ContainerRepository remote) {
        return Status.UNKNOWN;
    }

    public List<ImmutablePair<String, OffsetDateTime>> listTagsWithDates() {
        return this.remoteOptional(false)
            .map(r -> r.listManifestProperties().stream()
                .flatMap(p -> p.getTags().stream()
                    .map(t -> ImmutablePair.of(t, p.getLastUpdatedOn())))
                .collect(Collectors.toList())
            ).orElse(Collections.emptyList());
    }
}
