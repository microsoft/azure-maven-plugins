/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

abstract class AbstractAzureEntityManager<T extends IAzureEntityManager<E>, E extends AbstractAzureEntityManager.RemoteAwareResourceEntity<R>, R>
        implements IAzureEntityManager<E> {

    private boolean refreshed;
    @Nonnull
    protected final E entity;

    protected AbstractAzureEntityManager(@Nonnull E entity) {
        this.entity = entity;
    }

    public final boolean exists() {
        if (Objects.isNull(this.remote()) && !this.refreshed) {
            this.refresh();
        }
        return Objects.nonNull(this.remote());
    }

    @Nonnull
    @Override
    public synchronized T refresh() {
        return this.refresh(this.loadRemote());
    }

    protected T refresh(@Nullable R remote) {
        this.entity.setRemote(remote);
        this.refreshed = true;
        //noinspection unchecked
        return (T) this;
    }

    @Nonnull
    public final E entity() {
        return entity;
    }

    @Nullable
    final R remote() {
        return this.entity.getRemote();
    }

    @Nullable
    abstract R loadRemote();

    abstract static class RemoteAwareResourceEntity<R> implements IAzureResourceEntity {
        @Nullable
        @JsonIgnore
        @Getter(AccessLevel.PACKAGE)
        @Setter(AccessLevel.PACKAGE)
        protected transient R remote;
    }
}
