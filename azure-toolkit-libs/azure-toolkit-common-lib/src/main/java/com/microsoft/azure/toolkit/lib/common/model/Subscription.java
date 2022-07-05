/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subscription {
    @Nonnull
    @JsonProperty
    private String id;
    @JsonProperty
    private String name;
    @JsonProperty
    private String tenantId;
    @JsonProperty
    private boolean selected;

    public Subscription(com.azure.resourcemanager.resources.models.Subscription s) {
        this.id = s.subscriptionId();
        this.name = s.displayName();
        this.tenantId = s.innerModel().tenantId();
    }
}
