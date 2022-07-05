/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface AzResource<T extends AzResource<T, P, R>, P extends AzResource<P, ?, ?>, R>
    extends AzResourceBase, Refreshable {

    None NONE = new None();
    String RESOURCE_GROUP_PLACEHOLDER = "${rg}";

    boolean exists();

    void refresh();

    @Nonnull
    AzResourceModule<T, P, R> getModule();

    @Nonnull
    String getName();

    @Nonnull
    default String getFullResourceType() {
        return this.getModule().getFullResourceType();
    }

    @Nonnull
    default String getResourceTypeName() {
        return this.getModule().getResourceTypeName();
    }

    @Nonnull
    default String getId() {
        return String.format("%s/%s", this.getModule().getId(), this.getName());
    }

    @Nonnull
    default P getParent() {
        return this.getModule().getParent();
    }

    @Nonnull
    default String getSubscriptionId() {
        return this.getModule().getSubscriptionId();
    }

    @Nonnull
    default String getResourceGroupName() {
        return ResourceId.fromString(this.getId()).resourceGroupName();
    }

    @Nonnull
    AzResource.Draft<T, R> update();

    void delete();

    @Nullable
    R getRemote();

    @Nonnull
    String getStatus();

    @Nonnull
    default Subscription getSubscription() {
        return Azure.az(IAzureAccount.class).account().getSubscription(this.getSubscriptionId());
    }

    @Nonnull
    default String getPortalUrl() {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        Subscription subscription = account.getSubscription(this.getSubscriptionId());
        return String.format("%s/#@%s/resource%s", account.getPortalUrl(), subscription.getTenantId(), this.getId());
    }

    // ***** START! TO BE REMOVED ***** //
    @Deprecated
    default String name() {
        return this.getName();
    }

    @Deprecated
    default String id() {
        return this.getId();
    }

    // ***** END! TO BE REMOVED ***** //

    @Getter
    final class None extends AbstractAzResource<None, None, Void> {
        public static final String NONE = "$NONE$";
        private final String id = NONE;
        private final String name = NONE;
        private final String status = NONE;
        private final String subscriptionId = NONE;

        private None() {
            super("$NONE$", AzResource.RESOURCE_GROUP_PLACEHOLDER, AzResourceModule.NONE);
        }

        @Nonnull
        @Override
        public List<AbstractAzResourceModule<?, None, ?>> getSubModules() {
            return Collections.emptyList();
        }

        @Nonnull
        @Override
        public AbstractAzResourceModule<None, None, Void> getModule() {
            return AzResourceModule.NONE;
        }

        @Nonnull
        @Override
        public String getFullResourceType() {
            return NONE;
        }

        @Nonnull
        @Override
        public String getResourceTypeName() {
            return NONE;
        }

        @Nonnull
        @Override
        public String loadStatus(@Nonnull Void remote) {
            return Status.UNKNOWN;
        }

        @Override
        public boolean equals(Object o) {
            return AzResource.NONE == o;
        }

        @Override
        public int hashCode() {
            return this.getClass().hashCode();
        }
    }

    interface Draft<T extends AzResource<T, ?, R>, R> {

        String getName();

        String getResourceGroupName();

        AzResourceModule<T, ?, R> getModule();

        default T commit() {
            synchronized (this) {
                final boolean existing = this.getModule().exists(this.getName(), this.getResourceGroupName());
                final T result = existing ? this.getModule().update(this) : this.getModule().create(this);
                this.reset();
                return result;
            }
        }

        void reset();

        @Nonnull
        default T createIfNotExist() {
            synchronized (this) {
                final T origin = this.getModule().get(this.getName(), this.getResourceGroupName());
                if (Objects.isNull(origin) || !origin.exists()) {
                    return this.getModule().create(this);
                }
                return origin;
            }
        }

        @Nullable
        default T updateIfExist() {
            synchronized (this) {
                final T origin = this.getModule().get(this.getName(), this.getResourceGroupName());
                if (Objects.nonNull(origin) && origin.exists()) {
                    return this.getModule().update(this);
                }
                return origin;
            }
        }

        @Nonnull
        R createResourceInAzure();

        default T asResource() {
            //noinspection unchecked
            return (T) this;
        }

        @Nonnull
        R updateResourceInAzure(@Nonnull R origin);

        boolean isModified();

        @Nullable
        T getOrigin();
    }

    interface Status {
        // unstable states
        String PENDING = "Pending";

        String CREATING = "Creating";
        String DELETING = "Deleting";
        String LOADING = "Loading";
        String UPDATING = "Updating";
        String SCALING = "Scaling";
        String DEPLOYING = "Deploying";

        String STARTING = "Starting";
        String RESTARTING = "Restarting";
        String STOPPING = "Stopping";

        // stable states
        String DELETED = "Deleted";
        String ERROR = "Error";
        String INACTIVE = "Inactive"; // no active deployment/...
        String RUNNING = "Running";
        String STOPPED = "Stopped";
        String UNKNOWN = "Unknown";
    }
}
