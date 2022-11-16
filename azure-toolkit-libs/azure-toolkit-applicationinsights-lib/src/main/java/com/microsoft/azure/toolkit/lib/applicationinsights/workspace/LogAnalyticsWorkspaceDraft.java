package com.microsoft.azure.toolkit.lib.applicationinsights.workspace;

import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.azure.resourcemanager.loganalytics.models.Workspace;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class LogAnalyticsWorkspaceDraft extends LogAnalyticsWorkspace implements AzResource.Draft<LogAnalyticsWorkspace, Workspace> {
    private static final String START_CREATING_LOG_ANALYTICS_WORKSPACE = "Start creating Log Analytics workspace ({0})...";
    private static final String REGION_IS_REQUIRED = "'region' is required to create Log Analytics workspace.";
    private static final String APPLICATION_INSIGHTS_CREATED = "Log Analytics workspace ({0}) is successfully created.";

    @Setter
    @Nullable
    private Region region;
    @Getter
    @Nullable
    private final LogAnalyticsWorkspace origin;

    protected LogAnalyticsWorkspaceDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull LogAnalyticsWorkspaceModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    protected LogAnalyticsWorkspaceDraft(@Nonnull LogAnalyticsWorkspace origin) {
        super(origin);
        this.origin = origin;
    }


    @Override
    public void reset() {
        this.region = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "workspace.create_log_analytics_workspace_in_azure.workspace", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public Workspace createResourceInAzure() {
        if (Objects.isNull(region)) {
            throw new AzureToolkitRuntimeException(REGION_IS_REQUIRED);
        }
        final LogAnalyticsManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format(START_CREATING_LOG_ANALYTICS_WORKSPACE, getName()));
        final Workspace workspace = manager.workspaces().define(getName())
                .withRegion(region.getName())
                .withExistingResourceGroup(getResourceGroupName())
                .create();
        messager.success(AzureString.format(APPLICATION_INSIGHTS_CREATED, getName()));
        return workspace;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "workspace.update_log_analytics_workspace_in_azure.workspace", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public Workspace updateResourceInAzure(@Nonnull Workspace origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @javax.annotation.Nullable
    @Override
    public Region getRegion() {
        return Optional.ofNullable(this.region).orElseGet(super::getRegion);
    }

    @Override
    public boolean isModified() {
        return this.region != null && !Objects.equals(this.region, this.origin.getRegion());
    }

}
