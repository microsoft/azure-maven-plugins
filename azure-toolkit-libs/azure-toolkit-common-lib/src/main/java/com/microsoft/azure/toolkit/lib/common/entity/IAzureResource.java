/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import java.beans.PropertyChangeListener;

public interface IAzureResource<T extends IAzureResourceEntity> {
    String PROPERTY_STATUS = "status";
    String PROPERTY_CHILDREN = "children";
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
