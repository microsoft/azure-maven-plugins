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

    String getResourceGroupName();

    String getStatus();

    Subscription getSubscription();

    String getPortalUrl();

    default FormalStatus getFormalStatus() {
        return StringUtils.isBlank(this.getStatus()) ? FormalStatus.UNKNOWN : FormalStatus.dummyFormalize(this.getStatus());
    }

    enum FormalStatus {
        RUNNING, STOPPED, FAILED, UNKNOWN, WRITING, READING;

        private static final HashSet<String> runningStatus = Sets.newHashSet("running", "success", "succeeded", "ready", "ok");
        private static final HashSet<String> stoppedStatus = Sets.newHashSet("stopped");
        private static final HashSet<String> failedStatus = Sets.newHashSet("failed", "error");
        private static final HashSet<String> writingStatus = Sets.newHashSet("writing", "pending", "processing", "creating", "updating", "deleting",
            "starting", "stopping", "restarting", "scaling");
        private static final HashSet<String> readingStatus = Sets.newHashSet("reading", "loading", "refreshing");

        public static FormalStatus dummyFormalize(String status) {
            status = status.toLowerCase();
            if (runningStatus.contains(status)) {
                return FormalStatus.RUNNING;
            } else if (stoppedStatus.contains(status)) {
                return FormalStatus.STOPPED;
            } else if (failedStatus.contains(status)) {
                return FormalStatus.FAILED;
            } else if (writingStatus.contains(status)) {
                return FormalStatus.WRITING;
            } else if (readingStatus.contains(status)) {
                return FormalStatus.READING;
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

        public boolean isWriting() {
            return this == WRITING;
        }

        public boolean isReading() {
            return this == READING;
        }

        public boolean isWaiting() {
            return this == WRITING || this == READING;
        }

        public boolean isUnknown() {
            return this == UNKNOWN;
        }
    }
}
