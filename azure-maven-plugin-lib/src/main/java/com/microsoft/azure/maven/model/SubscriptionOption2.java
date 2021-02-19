/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
// todo: Remove duplicated codes for track1 and track2 subscription
package com.microsoft.azure.maven.model;

import com.azure.resourcemanager.resources.models.Subscription;
import org.apache.commons.lang3.StringUtils;

public class SubscriptionOption2 implements Comparable<SubscriptionOption2> {

    private Subscription inner;

    public SubscriptionOption2(Subscription inner) {
        this.inner = inner;
    }

    public Subscription getSubscription() {
        return inner;
    }

    public String getSubscriptionId() {
        return inner != null ? inner.subscriptionId() : null;
    }

    public String getSubscriptionName() {
        return inner != null ? inner.displayName() : null;
    }

    @Override
    public String toString() {
        return inner != null ? getSubscriptionName(this.inner) : null;
    }

    @Override
    public int compareTo(SubscriptionOption2 other) {
        final String name1 = inner != null ? inner.displayName() : null;
        final String name2 = other.inner != null ? other.inner.displayName() : null;
        return StringUtils.compare(name1, name2);
    }

    public static String getSubscriptionName(Subscription subs) {
        return String.format("%s(%s)", subs.displayName(), subs.subscriptionId());
    }
}
