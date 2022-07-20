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
import java.util.List;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class OutboundType {
    public static final OutboundType LOAD_BALANCER = new OutboundType("loadBalancer");
    public static final OutboundType USER_DEFINED_ROUTING = new OutboundType("userDefinedRouting");
    public static final OutboundType MANAGED_NATGATEWAY = new OutboundType("managedNATGateway");
    public static final OutboundType USER_ASSIGNED_NATGATEWAY = new OutboundType("userAssignedNATGateway");


    private String value;

    public static List<OutboundType> values() {
        return Arrays.asList(LOAD_BALANCER, USER_DEFINED_ROUTING, MANAGED_NATGATEWAY, USER_ASSIGNED_NATGATEWAY);
    }

    public static OutboundType fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElse(null);
    }
}
