/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.vm;

import com.azure.resourcemanager.compute.models.ComputeSku;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class AzureVirtualMachineSize {
    private final String name;

    public AzureVirtualMachineSize(final ComputeSku size) {
        this.name = size.name().toString();
    }

    public AzureVirtualMachineSize(final String name) {
        this.name = name;
    }
}
