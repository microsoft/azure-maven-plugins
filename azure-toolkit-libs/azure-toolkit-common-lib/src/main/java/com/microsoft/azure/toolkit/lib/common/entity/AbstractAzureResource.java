/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

public abstract class AbstractAzureResource<T extends IAzureResource<E>, E extends AbstractAzureResource.RemoteAwareResourceEntity<R>, R>
        implements IAzureResource<E> {

    private boolean refreshed;
    @Nonnull
    protected final E entity;
    protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private String status = null;

    protected AbstractAzureResource(@Nonnull E entity) {
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
        this.refreshStatus(Status.LOADING);
        return this.refresh(this.loadRemote());
    }

    protected T refresh(@Nullable R remote) {
        this.entity.setRemote(remote);
        this.refreshed = true;
        this.refreshStatus();
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

    public final void refreshChildren() {
        this.refresh();
        this.pcs.firePropertyChange(PROPERTY_CHILDREN, 0, 1);
    }

    @Override
    public final String status() {
        if (Objects.nonNull(this.status)) {
            return this.status;
        } else {
            this.refreshStatus();
            return Status.LOADING;
        }
    }

    public final void refreshStatus() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> this.refreshStatus(this.loadStatus()));
    }

    protected final void refreshStatus(@Nonnull String status) {
        final String oldStatus = this.status;
        this.status = status;
        if (!StringUtils.equalsIgnoreCase(oldStatus, this.status)) {
            this.pcs.firePropertyChange(PROPERTY_STATUS, oldStatus, this.status);
        }
    }

    @Nullable
    protected abstract R loadRemote();

    /**
     * @return {@link Status}
     */
    protected String loadStatus() {
        return Status.RUNNING;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    public abstract static class RemoteAwareResourceEntity<R> implements IAzureResourceEntity {
        @Nullable
        @JsonIgnore
        @Getter(AccessLevel.PUBLIC)
        @Setter(AccessLevel.PUBLIC)
        protected transient R remote;
    }
}
