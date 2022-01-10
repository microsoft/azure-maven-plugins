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

    default String getFormalStatus() {
        return this.status();
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
        String PENDING = "PENDING";

        String CREATING = "CREATING";
        String DELETING = "DELETING";
        String LOADING = "LOADING";
        String UPDATING = "UPDATING";
        String SCALING = "SCALING";

        String STARTING = "STARTING";
        String RESTARTING = "RESTARTING";
        String STOPPING = "STOPPING";

        // Draft
        String DRAFT = "DRAFT";
        String NULL = "NULL";

        // stable states
        String STABLE = "STABLE";
        String DELETED = "DELETED";
        String ERROR = "ERROR";
        String DISCONNECTED = "DISCONNECTED"; // failed to get remote/client
        String INACTIVE = "INACTIVE"; // no active deployment/...
        String RUNNING = "RUNNING";
        String STOPPED = "STOPPED";
        String UNKNOWN = "UNKNOWN";

        List<String> status = Arrays.asList(UNSTABLE, PENDING, DRAFT, STABLE, LOADING, ERROR, RUNNING, STOPPED, UNKNOWN);
    }
}
