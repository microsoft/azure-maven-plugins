package com.microsoft.azure.toolkit.lib.applicationinsights.workspace;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LogAnalyticsWorkspaceConfig {
    private boolean newCreate;
    @EqualsAndHashCode.Include
    private String name;
    @EqualsAndHashCode.Include
    private String resourceId;
    private String subscriptionId;
    private String regionName;
}
