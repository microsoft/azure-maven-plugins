package com.microsoft.azure.toolkit.lib.monitor;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class LogAnalyticsWorkspaceConfig {
    private boolean newCreate;
    @EqualsAndHashCode.Include
    private String name;
    @EqualsAndHashCode.Include
    private String resourceId;
    private String subscriptionId;
    private String regionName;

    public static LogAnalyticsWorkspaceConfig createConfig(Subscription subscription, Region region) {
        final String defaultWorkspaceName = String.format("DefaultWorkspace-%s-%s", subscription.getId(), Optional.ofNullable(region).map(Region::getAbbreviation).orElse(StringUtils.EMPTY));
        final String finalWorkspaceName = defaultWorkspaceName.length() > 63 ? defaultWorkspaceName.substring(0, 63) : defaultWorkspaceName;
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
}
