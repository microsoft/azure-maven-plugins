/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import com.azure.core.management.AzureEnvironment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureCliSubscription extends Subscription {
    @JsonProperty
    private String homeTenantId;
    @JsonProperty
    private String state;
    @JsonProperty("isDefault")
    private boolean selected;
    @JsonIgnore
    private AzureEnvironment environment;
    @JsonIgnore
    private String email;

    @JsonProperty("user")
    public void setUser(Map<String, String> user) {
        this.email = user.get("name");
    }

    @JsonProperty("cloudName")
    public void setCloudName(String cloudName) {
        this.environment = AzureEnvironmentUtils.stringToAzureEnvironment(cloudName);
    }
}
