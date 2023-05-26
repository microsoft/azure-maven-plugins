/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subscription {
    public static final Subscription NONE = Subscription.builder().name(AzResource.NONE.getName())
        .id("00000000-0000-0000-0000-000000000000").tenantId("00000000-0000-0000-0000-000000000000").selected(true).build();
    @Nonnull
    @JsonProperty
    @EqualsAndHashCode.Include
    private String id;
    @JsonProperty
    private String name;
    @JsonProperty
    private String tenantId;
    @JsonProperty
    private boolean selected;

    public Subscription(@Nonnull String id) {
        this.id = id;
    }

    public Subscription(com.azure.resourcemanager.resources.models.Subscription s) {
        this.id = s.subscriptionId();
        this.name = s.displayName();
        this.tenantId = s.innerModel().tenantId();
    }
}
