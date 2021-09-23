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
    public static final AzureVirtualMachineSize Standard_D2 = new AzureVirtualMachineSize("Standard_D2");
    public static final AzureVirtualMachineSize Standard_D2_v2 = new AzureVirtualMachineSize("Standard_D2_v2");
    public static final AzureVirtualMachineSize Standard_DS2 = new AzureVirtualMachineSize("Standard_DS2");
    public static final AzureVirtualMachineSize Standard_D2s_v3 = new AzureVirtualMachineSize("Standard_D2s_v3");
    public static final AzureVirtualMachineSize Standard_D4s_v3 = new AzureVirtualMachineSize("Standard_D4s_v3");
    public static final AzureVirtualMachineSize Standard_E2s_v3 = new AzureVirtualMachineSize("Standard_E2s_v3");

    private final String name;

    public AzureVirtualMachineSize(final ComputeSku size) {
        this.name = size.name().toString();
    }

    public AzureVirtualMachineSize(final String name) {
        this.name = name;
    }
}
