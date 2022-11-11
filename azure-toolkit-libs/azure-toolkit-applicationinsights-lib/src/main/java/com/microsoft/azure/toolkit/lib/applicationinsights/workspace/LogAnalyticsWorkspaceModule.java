package com.microsoft.azure.toolkit.lib.applicationinsights.workspace;

import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.azure.resourcemanager.loganalytics.models.Workspace;
import com.azure.resourcemanager.loganalytics.models.Workspaces;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class LogAnalyticsWorkspaceModule extends AbstractAzResourceModule<LogAnalyticsWorkspace, LogAnalyticsServiceWorkspaceSubscription, Workspace> {
    public static final String NAME = "workspaces";

    public LogAnalyticsWorkspaceModule(@Nonnull LogAnalyticsServiceWorkspaceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.load_resources_in_azure.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected Stream<Workspace> loadResourcesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(workspaces -> workspaces.list().stream()).orElse(Stream.empty());

    }

    @Nullable
    @Override
    @AzureOperation(name = "resource.load_resource_in_azure.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected Workspace loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(workspaces -> workspaces.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "workspace.delete_log_analytics_workspace_in_azure.workspace", params = {"nameFromResourceId(resourceId)"}, type = AzureOperation.Type.REQUEST)
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(workspaces -> workspaces.deleteById(resourceId));
    }

    @Nonnull
    @Override
    protected LogAnalyticsWorkspaceDraft newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        assert rgName != null : "'Resource group' is required.";
        return new LogAnalyticsWorkspaceDraft(name, rgName, this);
    }

    @Nonnull
    @Override
    protected LogAnalyticsWorkspaceDraft newDraftForUpdate(@Nonnull LogAnalyticsWorkspace workspace) {
        return new LogAnalyticsWorkspaceDraft(workspace);
    }

    @Nonnull
    @Override
    protected LogAnalyticsWorkspace newResource(@Nonnull Workspace remote) {
        return new LogAnalyticsWorkspace(remote, this);
    }

    @Nonnull
    @Override
    protected LogAnalyticsWorkspace newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new LogAnalyticsWorkspace(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nullable
    @Override
    protected Workspaces getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(LogAnalyticsManager::workspaces).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Log Analytics workspace";
    }
}
