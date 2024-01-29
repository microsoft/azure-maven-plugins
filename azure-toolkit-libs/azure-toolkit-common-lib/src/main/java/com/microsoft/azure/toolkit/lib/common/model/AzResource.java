/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.google.common.collect.Sets;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public interface AzResource extends Refreshable, AzComponent {
    Action.Id<AzResource> CONNECT_RESOURCE = Action.Id.of("user/$resource.connect_resource.resource");
    Action.Id<Object> CREATE_RESOURCE = Action.Id.of("user/$resource.create_resource.type");
    Action.Id<AzResource> DEPLOY = Action.Id.of("user/$resource.deploy_resource.resource");

    long CACHE_LIFETIME = 30 * 60 * 1000; // 30 minutes

    None NONE = new None();
    String RESOURCE_GROUP_PLACEHOLDER = "${rg}";

    boolean exists();

    void refresh();

    @Nonnull
    AzResourceModule<?> getModule();

    @Nonnull
    default String getFullResourceType() {
        return this.getModule().getFullResourceType();
    }

    @Nonnull
    default String getResourceTypeName() {
        return this.getModule().getResourceTypeName();
    }

    @Nonnull
    default String getSubscriptionId() {
        return this.getModule().getSubscriptionId();
    }

    @Nonnull
    default String getResourceGroupName() {
        return ResourceId.fromString(this.getId()).resourceGroupName();
    }

    void delete();

    @Nonnull
    default Subscription getSubscription() {
        try {
            return Azure.az(IAzureAccount.class).account().getSubscription(this.getSubscriptionId());
        } catch (final IllegalArgumentException e) {
            return new Subscription(this.getSubscriptionId());
        }
    }

    @Nonnull
    default String getPortalUrl() {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        final Subscription subscription = account.getSubscription(this.getSubscriptionId());
        return String.format("%s/#@%s/resource%s", account.getPortalUrl(), subscription.getTenantId(), this.getId());
    }

    @Nonnull
    String getStatus();

    default FormalStatus getFormalStatus() {
        final String status = this.getStatus();
        return StringUtils.isBlank(status) ? FormalStatus.UNKNOWN : FormalStatus.dummyFormalize(status);
    }

    @SuppressWarnings("unused")
    enum FormalStatus {
        RUNNING, STOPPED, FAILED, DELETED, UNKNOWN, WRITING, READING, CREATING, DELETING;

        private static final HashSet<String> runningStatus = Sets.newHashSet("running", "success", "succeeded", "ready", "ok", "online", "healthy", "active");
        private static final HashSet<String> stoppedStatus = Sets.newHashSet("stopped", "deallocated", "deprovisioned", "disabled");
        private static final HashSet<String> failedStatus = Sets.newHashSet("failed", "error", "unhealthy");
        private static final HashSet<String> writingStatus = Sets.newHashSet("writing", "pending", "processing", "updating",
            "starting", "stopping", "activating", "deactivating", "restarting", "scaling", "deprovisioning", "provisioning", "deploying");
        private static final HashSet<String> readingStatus = Sets.newHashSet("reading", "loading", "refreshing");
        private static final HashSet<String> deletingStatus = Sets.newHashSet("deleting");
        private static final HashSet<String> deletedStatus = Sets.newHashSet("deleted", "removed", "disconnected");

        public static FormalStatus dummyFormalize(String status) {
            status = status.toLowerCase();
            if (runningStatus.contains(status)) {
                return FormalStatus.RUNNING;
            } else if (stoppedStatus.contains(status)) {
                return FormalStatus.STOPPED;
            } else if (failedStatus.contains(status)) {
                return FormalStatus.FAILED;
            } else if ("creating".equals(status)) {
                return FormalStatus.CREATING;
            } else if (writingStatus.contains(status)) {
                return FormalStatus.WRITING;
            } else if (readingStatus.contains(status)) {
                return FormalStatus.READING;
            } else if (deletingStatus.contains(status)) {
                return FormalStatus.DELETING;
            } else if (deletedStatus.contains(status)) {
                return FormalStatus.DELETED;
            } else {
                return FormalStatus.UNKNOWN;
            }
        }

        public boolean isRunning() {
            return this == RUNNING;
        }

        public boolean isStopped() {
            return this == STOPPED;
        }

        public boolean isFailed() {
            return this == FAILED;
        }

        public boolean isDeleted() {
            return this == DELETED;
        }

        public boolean isCreating() {
            return this == CREATING;
        }

        public boolean isDeleting() {
            return this == DELETING;
        }

        public boolean isWriting() {
            return this == WRITING || this.isCreating();
        }

        public boolean isReading() {
            return this == READING;
        }

        public boolean isWaiting() {
            return this.isWriting() || this.isDeleting() || this.isCreating() || this.isReading();
        }

        public boolean isUnknown() {
            return this == UNKNOWN;
        }

        public boolean isWritable() {
            return this.isConnected() && !(this.isFailed() || this.isWriting());
        }

        public boolean isConnected() {
            return !(this.isDeleted() || this.isUnknown() || this.isCreating() || this.isDeleting());
        }
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
        public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
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
        protected String loadStatus(@Nonnull Void remote) {
            return Status.OK;
        }

        @Nonnull
        @Override
        public String getStatus() {
            return Status.OK;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof None;
        }

        @Override
        public int hashCode() {
            return this.getClass().hashCode();
        }
    }

    @SuppressWarnings("unused")
    interface Draft<T extends AzResource, R> {

        String getName();

        String getResourceGroupName();

        AzResourceModule<T> getModule();

        default T commit() {
            final boolean existing = this.getModule().exists(this.getName(), this.getResourceGroupName());
            final T result = existing ? this.getModule().update(this) : this.getModule().create(this);
            this.reset();
            return result;
        }

        void reset();

        @Nonnull
        default T createIfNotExist() {
            final T origin = this.getModule().get(this.getName(), this.getResourceGroupName());
            if (Objects.isNull(origin) || !origin.exists()) {
                return this.getModule().create(this);
            }
            return origin;
        }

        @Nullable
        default T updateIfExist() {
            final T origin = this.getModule().get(this.getName(), this.getResourceGroupName());
            if (Objects.nonNull(origin) && origin.exists()) {
                return this.getModule().update(this);
            }
            return origin;
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

    @SuppressWarnings("unused")
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
        String ACTIVATING = "Activating";
        String DEACTIVATING = "Deactivating";

        // stable states
        String DELETED = "Deleted";
        String ERROR = "Error";
        String INACTIVE = "Inactive"; // no active deployment/...
        String RUNNING = "Running";
        String OK = "OK";
        String ONLINE = "Online";
        String STOPPED = "Stopped";
        String UNKNOWN = "Unknown";
    }
}
