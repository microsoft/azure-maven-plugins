/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.applicationinsights;

import com.azure.resourcemanager.applicationinsights.models.ApplicationInsightsComponent;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ApplicationInsight extends AbstractAzResource<ApplicationInsight, ApplicationInsightsServiceSubscription, ApplicationInsightsComponent> {
    protected ApplicationInsight(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ApplicationInsightsModule module) {
        super(name, resourceGroupName, module);
    }

    protected ApplicationInsight(@Nonnull ApplicationInsight insight) {
        super(insight);
    }

    protected ApplicationInsight(@Nonnull ApplicationInsightsComponent remote, @Nonnull ApplicationInsightsModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.setRemote(remote);
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(component -> Region.fromName(component.name())).orElse(null);
    }

    @Nullable
    public String getType() {
        return Optional.ofNullable(getRemote()).map(ApplicationInsightsComponent::type).orElse(null);
    }

    @Nullable
    public String getKind() {
        return Optional.ofNullable(getRemote()).map(ApplicationInsightsComponent::kind).orElse(null);
    }

    @Nullable
    public String getInstrumentationKey() {
        return Optional.ofNullable(getRemote()).map(ApplicationInsightsComponent::instrumentationKey).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ApplicationInsight, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull ApplicationInsightsComponent remote) {
        return remote.provisioningState();
    }
}
