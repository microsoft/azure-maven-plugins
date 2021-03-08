/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@RequiredArgsConstructor
public abstract class SubscriptionScoped<T extends AzureService> {
    @Nonnull
    private final Function<List<Subscription>, T> creator;
    @Nullable
    private final List<Subscription> subscriptions;

    protected SubscriptionScoped(@Nonnull final Function<List<Subscription>, T> creator) {
        this.creator = creator;
        this.subscriptions = null;
    }

    public List<Subscription> getSubscriptions() {
        if (Objects.isNull(this.subscriptions)) {
            return Azure.az(IAzureAccount.class).account().getSelectedSubscriptions(); // user may reselect subscriptions
        }
        return this.subscriptions;
    }

    public T subscriptions(@Nonnull final List<Subscription> subscriptions) {
        assert CollectionUtils.isNotEmpty(subscriptions) : "subscriptions can not be empty!";
        return this.creator.apply(subscriptions);
    }

    public T subscription(@Nonnull final Subscription subscription) {
        return this.subscriptions(Collections.singletonList(subscription));
    }
}
