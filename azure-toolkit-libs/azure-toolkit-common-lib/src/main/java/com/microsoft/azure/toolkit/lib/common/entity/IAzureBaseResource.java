/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public interface IAzureBaseResource<T extends IAzureBaseResource, P extends IAzureBaseResource> extends AzResourceBase {
    String REST_SEGMENT_JOB_MANAGEMENT_TENANTID = "/#@";
    String REST_SEGMENT_JOB_MANAGEMENT_RESOURCE = "/resource";

    void refresh();

    boolean exists();

    @Nonnull
    default String getName() {
        return this.name();
    }

    @Nonnull
    default String getId() {
        return this.id();
    }

    @Nonnull
    default String getSubscriptionId() {
        return this.subscriptionId();
    }

    default String getResourceGroupName() {
        return this.resourceGroup();
    }

    default String getStatus() {
        return this.status();
    }

    default Subscription getSubscription() {
        return this.subscription();
    }

    default String getPortalUrl() {
        return this.portalUrl();
    }

    @Nullable
    @Deprecated
    default P parent() {
        return null;
    }

    // todo: Change to Nonnull
    @Nullable
    @Deprecated
    default IAzureModule<? extends T, ? extends P> module() {
        return null;
    }

    @Deprecated
    String name();

    @Deprecated
    String id();

    @Deprecated
    default String status() {
        return null;
    }

    @Deprecated
    default void refreshStatus() {
    }

    @Deprecated
    default String subscriptionId() {
        return ResourceId.fromString(id()).subscriptionId();
    }

    @Deprecated
    default String resourceGroup() {
        return ResourceId.fromString(id()).resourceGroupName();
    }

    @Deprecated
    default Subscription subscription() {
        return Azure.az(IAzureAccount.class).account().getSubscription(this.subscriptionId());
    }

    default String portalUrl() {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        Subscription subscription = account.getSubscription(this.subscriptionId());
        return account.portalUrl() + REST_SEGMENT_JOB_MANAGEMENT_TENANTID + subscription.getTenantId() + REST_SEGMENT_JOB_MANAGEMENT_RESOURCE + this.id();
    }

    interface Status {
        // unstable states
        String UNSTABLE = "UNSTABLE";
        String PENDING = "Pending";

        String CREATING = "Creating";
        String DELETING = "Deleting";
        String LOADING = "Loading";
        String UPDATING = "Updating";
        String SCALING = "Scaling";

        String STARTING = "Starting";
        String RESTARTING = "Restarting";
        String STOPPING = "Stopping";

        // Draft
        String DRAFT = "Draft";
        String NULL = "NULL";

        // stable states
        String STABLE = "STABLE";
        String DELETED = "Deleted";
        String ERROR = "Error";
        String DISCONNECTED = "Disconnected"; // failed to get remote/client
        String INACTIVE = "Inactive"; // no active deployment/...
        String RUNNING = "Running";
        String STOPPED = "Stopped";
        String UNKNOWN = "Unknown";

        List<String> status = Arrays.asList(UNSTABLE, PENDING, DRAFT, STABLE, LOADING, ERROR, RUNNING, STOPPED, UNKNOWN);
    }
}
