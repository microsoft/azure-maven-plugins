package com.microsoft.azure.toolkit.lib.applicationinsights.workspace;

import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class LogAnalyticsServiceWorkspaceSubscription extends AbstractAzServiceSubscription<LogAnalyticsServiceWorkspaceSubscription, LogAnalyticsManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final LogAnalyticsWorkspaceModule logAnalyticsWorkspaceModule;

    protected LogAnalyticsServiceWorkspaceSubscription(@Nonnull String subscriptionId, @Nonnull AzureLogAnalyticsWorkspace service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.logAnalyticsWorkspaceModule = new LogAnalyticsWorkspaceModule(this);
    }

    public LogAnalyticsWorkspaceModule logAnalyticsWorkspaces() {
        return this.logAnalyticsWorkspaceModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(logAnalyticsWorkspaceModule);
    }
}
