package com.microsoft.azure.toolkit.lib.applicationinsights.workspace;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@SuperBuilder
public class LogAnalyticsWorkspaceConfig {
    private boolean newCreate;
    private String name;
    private String resourceId;
    private String subscriptionId;
    private String regionName;

    public static LogAnalyticsWorkspaceConfig createConfig(Subscription subscription, Region region) {
        final String defaultWorkspaceName = String.format("DefaultWorkspace-%s-%s", subscription.getId(), Optional.ofNullable(region).map(Region::getAbbreviation).orElse(StringUtils.EMPTY));
        final String finalWorkspaceName = defaultWorkspaceName.length() > 64 ? defaultWorkspaceName.substring(0, 64) : defaultWorkspaceName;
        final Optional<LogAnalyticsWorkspace> remoteWorkspace = Azure.az(AzureLogAnalyticsWorkspace.class)
                .logAnalyticsWorkspaces(subscription.getId()).list().stream()
                .filter(workspace -> Objects.equals(workspace.getName(), finalWorkspaceName)).findFirst();
        final LogAnalyticsWorkspaceConfig workspaceConfig = LogAnalyticsWorkspaceConfig.builder().newCreate(!remoteWorkspace.isPresent())
                .subscriptionId(subscription.getId())
                .name(finalWorkspaceName)
                .regionName(Optional.ofNullable(region).map(Region::getName).orElse(StringUtils.EMPTY))
                .build();
        remoteWorkspace.ifPresent(w -> workspaceConfig.setResourceId(w.getId()));
        return workspaceConfig;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LogAnalyticsWorkspaceConfig)) {
            return false;
        }
        LogAnalyticsWorkspaceConfig compObj = (LogAnalyticsWorkspaceConfig) obj;
        if (this.newCreate && compObj.newCreate) {
            return true;
        } else if (this.newCreate != compObj.newCreate) {
            return false;
        }
        return Objects.equals(this.resourceId, compObj.resourceId);
    }
}
