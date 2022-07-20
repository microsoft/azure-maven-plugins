/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerservice.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class VirtualMachineSize {
    public static final VirtualMachineSize STANDARD_B2MS = new VirtualMachineSize("Standard_B2ms");
    public static final VirtualMachineSize STANDARD_B2S = new VirtualMachineSize("Standard_B2s");
    public static final VirtualMachineSize STANDARD_B4MS = new VirtualMachineSize("Standard_B4ms");
    public static final VirtualMachineSize STANDARD_D2S_V3 = new VirtualMachineSize("Standard_D2s_v3");
    public static final VirtualMachineSize STANDARD_D4S_V3 = new VirtualMachineSize("Standard_D4s_v3");
    public static final VirtualMachineSize STANDARD_D8S_V3 = new VirtualMachineSize("Standard_D8s_v3");
    public static final VirtualMachineSize STANDARD_DS2_V2 = new VirtualMachineSize("Standard_DS2_v2");
    public static final VirtualMachineSize STANDARD_DS3_V2 = new VirtualMachineSize("Standard_DS3_v2");

    private static final List<VirtualMachineSize> values = Collections.unmodifiableList(Arrays.asList(STANDARD_B2MS, STANDARD_B2S,
            STANDARD_B4MS, STANDARD_D2S_V3, STANDARD_D4S_V3, STANDARD_D8S_V3, STANDARD_DS2_V2, STANDARD_DS3_V2));

    private String value;

    public static List<VirtualMachineSize> values() {
        return values;
    }

    public static VirtualMachineSize fromString(String value) {
        return values().stream().filter(size -> StringUtils.equals(value, size.getValue()))
                .findFirst().orElseGet(() -> new VirtualMachineSize(value));
    }
}
