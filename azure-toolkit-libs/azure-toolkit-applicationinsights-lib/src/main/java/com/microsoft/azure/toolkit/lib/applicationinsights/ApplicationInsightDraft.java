/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.applicationinsights;

import com.azure.resourcemanager.applicationinsights.ApplicationInsightsManager;
import com.azure.resourcemanager.applicationinsights.models.ApplicationInsightsComponent;
import com.azure.resourcemanager.applicationinsights.models.ApplicationType;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class ApplicationInsightDraft extends ApplicationInsight implements AzResource.Draft<ApplicationInsight, ApplicationInsightsComponent> {

    private static final String REGION_IS_REQUIRED = "'region' is required to create application insight.";
    private static final String START_CREATING_APPLICATION_INSIGHT = "Start creating Application Insight ({0})...";
    private static final String APPLICATION_INSIGHTS_CREATED = "Application Insight ({0}) is successfully created. " +
            "You can visit {1} to view your Application Insights component.";

    @Getter
    @Setter
    private Region region;

    @Getter
    private final ApplicationInsight origin;

    protected ApplicationInsightDraft(@Nonnull String name, @Nonnull String resourceGroupName, ApplicationInsightsModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    protected ApplicationInsightDraft(@Nonnull ApplicationInsight origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.region = null;
    }

    @Override
    @AzureOperation(
            name = "resource.create_resource.resource|type",
            params = {"this.getName()", "this.getResourceTypeName()"},
            type = AzureOperation.Type.SERVICE
    )
    public ApplicationInsightsComponent createResourceInAzure() {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        if (Objects.isNull(region)) {
            throw new AzureToolkitRuntimeException(REGION_IS_REQUIRED);
        }
        final ApplicationInsightsManager applicationInsightsManager = Objects.requireNonNull(this.getParent().getRemote());
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format(START_CREATING_APPLICATION_INSIGHT, getName()));
        final ApplicationInsightsComponent result = applicationInsightsManager.components().define(getName())
                .withRegion(region.getName())
                .withExistingResourceGroup(getResourceGroupName())
                .withKind("web")
                .withApplicationType(ApplicationType.WEB).create();
        messager.success(AzureString.format(APPLICATION_INSIGHTS_CREATED, getName(), getPortalUrl()));
        return result;
    }

    @Override
    @AzureOperation(
            name = "resource.update_resource.resource|type",
            params = {"this.getName()", "this.getResourceTypeName()"},
            type = AzureOperation.Type.SERVICE
    )
    public ApplicationInsightsComponent updateResourceInAzure(@Nonnull ApplicationInsightsComponent origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return this.region != null && !Objects.equals(this.region, this.origin);
    }

    @Nullable
    @Override
    public ApplicationInsight getOrigin() {
        return this.origin;
    }
}
