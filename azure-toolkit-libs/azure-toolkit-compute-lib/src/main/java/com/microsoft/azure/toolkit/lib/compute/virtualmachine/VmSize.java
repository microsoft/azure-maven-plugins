/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.virtualmachine;

import com.azure.resourcemanager.compute.models.ComputeSku;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class VmSize {
    public static final VmSize Standard_D2 = new VmSize("Standard_D2");
    public static final VmSize Standard_D2_v2 = new VmSize("Standard_D2_v2");
    public static final VmSize Standard_DS2 = new VmSize("Standard_DS2");
    public static final VmSize Standard_D2s_v3 = new VmSize("Standard_D2s_v3");
    public static final VmSize Standard_D4s_v3 = new VmSize("Standard_D4s_v3");
    public static final VmSize Standard_E2s_v3 = new VmSize("Standard_E2s_v3");

    private final String name;

    public VmSize(final ComputeSku size) {
        this.name = size.name().toString();
    }

    public VmSize(final String name) {
        this.name = name;
    }
}
