/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.applicationinsights;

import com.azure.resourcemanager.applicationinsights.models.ApplicationInsightsComponent;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ApplicationInsight extends AbstractAzResource<ApplicationInsight, ApplicationInsightsResourceManager, ApplicationInsightsComponent> {
    protected ApplicationInsight(@Nonnull String name, @Nonnull String resourceGroupName, ApplicationInsightsModule module) {
        super(name, resourceGroupName, module);
    }

    protected ApplicationInsight(@Nonnull ApplicationInsight insight) {
        super(insight);
    }

    protected ApplicationInsight(@Nonnull ApplicationInsightsComponent remote, ApplicationInsightsModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.setRemote(remote);
    }

    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(component -> Region.fromName(component.name())).orElse(null);
    }

    public String getType() {
        return Optional.ofNullable(getRemote()).map(ApplicationInsightsComponent::type).orElse(null);
    }

    public String getKind() {
        return Optional.ofNullable(getRemote()).map(ApplicationInsightsComponent::kind).orElse(null);
    }

    public String getInstrumentationKey() {
        return Optional.ofNullable(getRemote()).map(ApplicationInsightsComponent::instrumentationKey).orElse(null);
    }

    @Override
    public List<AzResourceModule<?, ApplicationInsight, ?>> getSubModules() {
        return Collections.EMPTY_LIST;
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull ApplicationInsightsComponent remote) {
        return remote.provisioningState();
    }
}
