/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

public interface IAzureResource<T extends IAzureResourceEntity> {

    String REST_SEGMENT_JOB_MANAGEMENT_TENANTID = "/#@";
    String REST_SEGMENT_JOB_MANAGEMENT_RESOURCE = "/resource";

    IAzureResource<T> refresh();

    boolean exists();

    T entity();

    default String status() {
        return null;
    }

    default void refreshStatus() {
    }

    default String name() {
        return this.entity().getName();
    }

    default String id() {
        return this.entity().getId();
    }

    default String subscriptionId() {
        return ResourceId.fromString(id()).subscriptionId();
    }

    default String resourceGroup() {
        return ResourceId.fromString(id()).resourceGroupName();
    }

    default Subscription subscription() {
        return Azure.az(IAzureAccount.class).account().getSubscription(this.subscriptionId());
    }

    default String portalUrl() {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        Subscription subscription = account.getSubscription(this.subscriptionId());
        return account.portalUrl()
                + REST_SEGMENT_JOB_MANAGEMENT_TENANTID
                + subscription.getTenantId()
                + REST_SEGMENT_JOB_MANAGEMENT_RESOURCE
                + this.id();
    }

    interface Status {
        // unstable states
        String UNSTABLE = "UNSTABLE";
        String PENDING = "PENDING";

        // stable states
        String STABLE = "STABLE";
        String LOADING = "LOADING";
        String ERROR = "ERROR";
        String RUNNING = "RUNNING";
        String STOPPED = "STOPPED";
        String UNKNOWN = "UNKNOWN";
    }
}
