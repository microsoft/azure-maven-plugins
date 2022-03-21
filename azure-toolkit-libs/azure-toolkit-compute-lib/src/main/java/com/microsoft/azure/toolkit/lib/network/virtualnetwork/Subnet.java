/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.virtualnetwork;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class Subnet {
    private final String name;
    private final String addressSpace;

    public Subnet(com.azure.resourcemanager.network.models.Subnet resource) {
        this.name = resource.name();
        this.addressSpace = resource.addressPrefix();
    }
}
