/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractAzureEntityManager<T extends IAzureEntityManager<E>, E extends AbstractAzureEntityManager.RemoteAwareResourceEntity<R>, R>
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
    public final R remote() {
        return this.entity.getRemote();
    }

    @Nullable
    protected abstract R loadRemote();

    public abstract static class RemoteAwareResourceEntity<R> implements IAzureResourceEntity {
        @Nullable
        @JsonIgnore
        @Getter(AccessLevel.PUBLIC)
        @Setter(AccessLevel.PUBLIC)
        protected transient R remote;
    }
}
