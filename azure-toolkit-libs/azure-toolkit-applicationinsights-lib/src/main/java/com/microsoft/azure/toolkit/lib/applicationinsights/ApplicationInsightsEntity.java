package com.microsoft.azure.toolkit.lib.applicationinsights;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class ApplicationInsightsEntity implements IAzureResourceEntity {
    private String id;
    private String name;
    private String subscriptionId;
    private String resourceGroup;
    private String instrumentationKey;
    private String type;
    private String kind;
    private Region region;
}
