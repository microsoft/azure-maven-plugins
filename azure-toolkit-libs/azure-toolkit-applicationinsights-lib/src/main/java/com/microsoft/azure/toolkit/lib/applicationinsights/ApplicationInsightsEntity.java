package com.microsoft.azure.toolkit.lib.applicationinsights;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class ApplicationInsightsEntity {
    private String subscriptionId;
    private String resourceId;
    private String resourceName;
    private String instrumentationKey;
    private String type;
    private String kind;
    private String location;
    private String resourceGroup;
}
