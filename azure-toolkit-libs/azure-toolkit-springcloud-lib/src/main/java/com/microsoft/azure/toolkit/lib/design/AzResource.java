/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.design;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AzResource<T extends AzResource<T, P, R>, P extends AzResource<P, ?, ?>, R> extends IAzureBaseResource<T, P> {
    None NONE = new None();
    String RESOURCE_GROUP_PLACEHOLDER = "${rg}";

    boolean exists();

    void refresh();

    @Nonnull
    AzResourceModule<T, P> getModule();

    @Nonnull
    String getName();

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

    @Nullable
    R getRemote();

    String getStatus();

    // ***** START! TO BE REMOVED ***** //
    @Deprecated
    default String name() {
        return this.getName();
    }

    @Deprecated
    default String id() {
        return this.getId();
    }

    @Deprecated
    default String status() {
        return getStatus();
    }

    @Deprecated
    default void refreshStatus() {
    }

    @Deprecated
    default String subscriptionId() {
        return getSubscriptionId();
    }

    @Deprecated
    default String resourceGroup() {
        return ResourceId.fromString(id()).resourceGroupName();
    }

    @Deprecated
    default Subscription subscription() {
        return Azure.az(IAzureAccount.class).account().getSubscription(this.subscriptionId());
    }
    // ***** END! TO BE REMOVED ***** //

    default String getPortalUrl() {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        Subscription subscription = account.getSubscription(this.getSubscriptionId());
        return account.portalUrl() + REST_SEGMENT_JOB_MANAGEMENT_TENANTID + subscription.getTenantId() + REST_SEGMENT_JOB_MANAGEMENT_RESOURCE + this.getId();
    }

    @Getter
    final class None extends AbstractAzResource<None, None, Void> {
        private None() {
            super("NONE", "NONE", AzResourceModule.NONE);
        }

        @Nonnull
        @Override
        public String loadStatus(@Nonnull Void remote) {
            return Status.UNKNOWN;
        }
    }
}
