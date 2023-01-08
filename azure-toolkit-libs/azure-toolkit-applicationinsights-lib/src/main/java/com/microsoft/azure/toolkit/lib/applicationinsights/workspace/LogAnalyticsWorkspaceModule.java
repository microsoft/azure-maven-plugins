package com.microsoft.azure.toolkit.lib.applicationinsights.workspace;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.azure.resourcemanager.loganalytics.models.Workspace;
import com.azure.resourcemanager.loganalytics.models.Workspaces;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
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
    protected Iterator<? extends ContinuablePage<String, Workspace>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().iterableByPage(getPageSize()).iterator()).orElse(Collections.emptyIterator());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/resource.load_resources.type", params = {"this.getResourceTypeName()"})
    protected Stream<Workspace> loadResourcesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(workspaces -> workspaces.list().stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"})
    protected Workspace loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(workspaces -> workspaces.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/workspace.delete_log_analytics_workspace.workspace", params = {"nameFromResourceId(resourceId)"})
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
