/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.applicationinsights;

import com.azure.resourcemanager.applicationinsights.ApplicationInsightsManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@Getter
public class ApplicationInsightsResourceManager extends AbstractAzResourceManager<ApplicationInsightsResourceManager, ApplicationInsightsManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final ApplicationInsightsModule applicationInsightsModule;

    protected ApplicationInsightsResourceManager(@Nonnull String subscriptionId, @Nonnull AzureApplicationInsights service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.applicationInsightsModule = new ApplicationInsightsModule(this);
    }

    protected ApplicationInsightsResourceManager(@Nonnull ApplicationInsightsManager manager, @Nonnull AzureApplicationInsights service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Nonnull
    public ApplicationInsightsModule applicationInsights() {
        return this.applicationInsightsModule;
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, ApplicationInsightsResourceManager, ?>> getSubModules() {
        return Collections.singletonList(applicationInsightsModule);
    }
}
