/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.compute.network.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Subnet {
    private final String name;
    private final String addressSpace;

    public Subnet(String name, String addressSpace) {
        this.name = name;
        this.addressSpace = addressSpace;
    }

    public Subnet(com.azure.resourcemanager.network.models.Subnet resource) {
        this.name = resource.name();
        this.addressSpace = resource.addressPrefix();
    }
}
