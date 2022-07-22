/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerservice.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode
public class ContainerServiceNetworkProfile {
    private NetworkPlugin networkPlugin;
    private NetworkPolicy networkPolicy;
    private NetworkMode networkMode;
    private String podCidr;
    private String serviceCidr;
    private String dnsServiceIp;
    private String dockerBridgeCidr;
    private OutboundType outboundType;
    private LoadBalancerSku loadBalancerSku;
    private List<String> podCidrs;
    private List<String> serviceCidrs;
    private List<IpFamily> ipFamilies;

    public static ContainerServiceNetworkProfile fromNetworkProfile(com.azure.resourcemanager.containerservice.models.ContainerServiceNetworkProfile profile) {
        final ContainerServiceNetworkProfile result = new ContainerServiceNetworkProfile();
        result.setNetworkPlugin(NetworkPlugin.fromString(Optional.ofNullable(profile.networkPlugin()).map(Objects::toString).orElse(null)));
        result.setNetworkPolicy(NetworkPolicy.fromString(Optional.ofNullable(profile.networkPolicy()).map(Objects::toString).orElse(null)));
        result.setNetworkMode(NetworkMode.fromString(Optional.ofNullable(profile.networkMode()).map(Objects::toString).orElse(null)));
        result.setPodCidr(profile.podCidr());
        result.setServiceCidr(profile.serviceCidr());
        result.setDnsServiceIp(profile.dnsServiceIp());
        result.setDockerBridgeCidr(profile.dockerBridgeCidr());
        result.setOutboundType(OutboundType.fromString(Optional.ofNullable(profile.outboundType()).map(Objects::toString).orElse(null)));
        result.setLoadBalancerSku(LoadBalancerSku.fromString(Optional.ofNullable(profile.loadBalancerSku()).map(Objects::toString).orElse(null)));
        result.setPodCidrs(profile.podCidrs());
        result.setServiceCidrs(profile.serviceCidrs());
        final List<IpFamily> ipFamilyList = Optional.ofNullable(profile.ipFamilies())
                .map(list -> list.stream().map(ip -> IpFamily.fromString(ip.toString())).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        result.setIpFamilies(ipFamilyList);
        return result;
    }
}
