package com.microsoft.azure.toolkit.lib.monitor;

import com.azure.core.util.Context;
import com.azure.monitor.query.LogsQueryClient;
import com.azure.monitor.query.models.LogsQueryOptions;
import com.azure.monitor.query.models.LogsTable;
import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.azure.resourcemanager.loganalytics.models.Column;
import com.azure.resourcemanager.loganalytics.models.Workspace;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class LogAnalyticsWorkspace extends AbstractAzResource<LogAnalyticsWorkspace, LogAnalyticsServiceWorkspaceSubscription, Workspace> implements Deletable {

    protected LogAnalyticsWorkspace(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull LogAnalyticsWorkspaceModule module) {
        super(name, resourceGroupName, module);
    }

    protected LogAnalyticsWorkspace(@Nonnull LogAnalyticsWorkspace workspace) {
        super(workspace);
    }

    protected LogAnalyticsWorkspace(@Nonnull Workspace remote, @Nonnull LogAnalyticsWorkspaceModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(workspace -> Region.fromName(workspace.regionName())).orElse(null);
    }

    @Nullable
    public String getCustomerId() {
        return Optional.ofNullable(getRemote()).map(Workspace::customerId).orElse(null);
    }

    @Nullable
    public String getPrimarySharedKeys() {
        final LogAnalyticsManager remote = this.getParent().getRemote();
        return Optional.ofNullable(remote).map(LogAnalyticsManager::sharedKeysOperations)
                .map(sharedKeysOperations -> sharedKeysOperations.getSharedKeys(this.getResourceGroupName(), this.getName()).primarySharedKey())
                .orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull Workspace remote) {
        return remote.provisioningState().toString();
    }

    @Nullable
    public LogsTable executeQuery(String queryString) {
        final LogsQueryOptions options = new LogsQueryOptions().setServerTimeout(Duration.ofSeconds(10));
        final String workspaceId = getCustomerId();
        final LogsQueryClient client = getParent().getLosQueryClient();
        if (Objects.nonNull(workspaceId) && Objects.nonNull(client)) {
            return client.queryWorkspaceWithResponse(workspaceId, queryString, null, options, Context.NONE).getValue().getTable();
        }
        return null;
    }

    public List<String> getTableColumnNames(String tableName) {
        final LogAnalyticsManager manager = getParent().getRemote();
        if (Objects.isNull(manager)) {
            return new ArrayList<>();
        }
        return manager.tables().get(getResourceGroupName(), getName(), tableName).schema().standardColumns()
                .stream().map(Column::name).collect(Collectors.toList());
    }
}
