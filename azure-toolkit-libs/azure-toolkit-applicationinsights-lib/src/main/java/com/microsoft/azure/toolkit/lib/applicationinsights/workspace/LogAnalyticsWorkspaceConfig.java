package com.microsoft.azure.toolkit.lib.applicationinsights.workspace;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LogAnalyticsWorkspaceConfig {
    private boolean newCreate;
    private String name;
    private String resourceId;
    private String resourceGroupName;
    private String subscriptionId;
}
