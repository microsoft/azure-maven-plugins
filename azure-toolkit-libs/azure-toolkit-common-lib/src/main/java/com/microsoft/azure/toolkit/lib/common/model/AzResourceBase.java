/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.HashSet;

public interface AzResourceBase {

    boolean exists();

    @Nonnull
    String getName();

    @Nonnull
    String getId();

    @Nonnull
    String getSubscriptionId();

    @Nonnull
    String getResourceGroupName();

    @Nonnull
    String getStatus();

    @Nonnull
    Subscription getSubscription();

    @Nonnull
    String getPortalUrl();

    default FormalStatus getFormalStatus() {
        return StringUtils.isBlank(this.getStatus()) ? FormalStatus.UNKNOWN : FormalStatus.dummyFormalize(this.getStatus());
    }

    enum FormalStatus {
        RUNNING, STOPPED, FAILED, DELETED, UNKNOWN, WRITING, READING, CREATING, DELETING;

        private static final HashSet<String> runningStatus = Sets.newHashSet("running", "success", "succeeded", "ready", "ok", "healthy");
        private static final HashSet<String> stoppedStatus = Sets.newHashSet("stopped", "deallocated", "deprovisioned");
        private static final HashSet<String> failedStatus = Sets.newHashSet("failed", "error", "unhealthy");
        private static final HashSet<String> writingStatus = Sets.newHashSet("writing", "pending", "processing", "updating",
            "starting", "stopping", "activating", "deactivating", "restarting", "scaling", "deprovisioning", "provisioning");
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
            } else if (status.equals("creating")) {
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
            return this.isWriting() || this.isDeleting() || this == READING;
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
}
