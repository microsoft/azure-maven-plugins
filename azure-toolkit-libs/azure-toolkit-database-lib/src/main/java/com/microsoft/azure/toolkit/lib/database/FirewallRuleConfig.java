/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
@Builder
public class FirewallRuleConfig {
    @Nonnull
    private String name;
    private String startIpAddress;
    private String endIpAddress;

}
