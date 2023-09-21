package com.microsoft.azure.toolkit.lib.monitor;

import com.azure.core.http.policy.HttpLogOptions;
import com.azure.monitor.query.LogsQueryClient;
import com.azure.monitor.query.LogsQueryClientBuilder;
import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Getter
public class LogAnalyticsServiceWorkspaceSubscription extends AbstractAzServiceSubscription<LogAnalyticsServiceWorkspaceSubscription, LogAnalyticsManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final LogAnalyticsWorkspaceModule logAnalyticsWorkspaceModule;
    @Nullable
    private final LogsQueryClient logsQueryClient;

    protected LogAnalyticsServiceWorkspaceSubscription(@Nonnull String subscriptionId, @Nonnull AzureLogAnalyticsWorkspace service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.logAnalyticsWorkspaceModule = new LogAnalyticsWorkspaceModule(this);
        this.logsQueryClient = new LogsQueryClientBuilder()
            .httpLogOptions(new HttpLogOptions().setLogLevel(Azure.az().config().getLogLevel()))
            .addPolicy(Azure.az().config().getUserAgentPolicy())
            .credential(Azure.az(AzureAccount.class).account().getTokenCredential(subscriptionId))
            .buildClient();
    }

    public LogAnalyticsWorkspaceModule logAnalyticsWorkspaces() {
        return this.logAnalyticsWorkspaceModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(logAnalyticsWorkspaceModule);
    }

    public LogsQueryClient getLosQueryClient() {
        return this.logsQueryClient;
    }
}
