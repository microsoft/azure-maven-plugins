/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.model;

import com.azure.resourcemanager.appcontainers.models.Ingress;
import com.azure.resourcemanager.appcontainers.models.IngressTransportMethod;
import com.azure.resourcemanager.appcontainers.models.TrafficWeight;
import lombok.*;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IngressConfig {
    public static final Traffic DEFAULT_TRAFFIC = Traffic.builder()
            .latestRevision(true).weight(100).build();
    private boolean enableIngress;
    private int targetPort;
    private boolean external;
    @Builder.Default
    private boolean allowInsecure = false;
    @Builder.Default
    private TransportMethod transport = TransportMethod.AUTO;
    @Builder.Default
    private List<Traffic> traffic = Arrays.asList(DEFAULT_TRAFFIC);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Traffic {
        private int weight;
        private boolean latestRevision;

        public static Traffic fromTrafficWeight(@Nonnull final TrafficWeight trafficWeight) {
            return Traffic.builder()
                    .weight(trafficWeight.weight())
                    .latestRevision(BooleanUtils.isTrue(trafficWeight.latestRevision()))
                    .build();
        }

        public TrafficWeight toTrafficWeight() {
            return new TrafficWeight()
                    .withWeight(weight)
                    .withLatestRevision(latestRevision);
        }
    }

    public static IngressConfig fromIngress(@Nullable final Ingress ingress) {
        if (ingress == null) {
            return IngressConfig.builder().enableIngress(false).build();
        }
        final List<Traffic> trafficList = Optional.ofNullable(ingress.traffic())
                .map(weight -> weight.stream().map(Traffic::fromTrafficWeight).collect(Collectors.toList()))
                .orElse(null);
        final TransportMethod transportMethod = Optional.ofNullable(ingress.transport())
                .map(method -> TransportMethod.fromString(method.toString())).orElse(null);
        return IngressConfig.builder()
                .enableIngress(true)
                .allowInsecure(ingress.allowInsecure())
                .external(ingress.external())
                .targetPort(ingress.targetPort())
                .traffic(trafficList)
                .transport(transportMethod)
                .build();
    }

    @Nullable
    public Ingress toIngress() {
        if (!isEnableIngress()) {
            return null;
        }
        final List<TrafficWeight> trafficList = Optional.ofNullable(traffic)
                .map(weight -> weight.stream().map(Traffic::toTrafficWeight).collect(Collectors.toList()))
                .orElse(null);
        final IngressTransportMethod transportMethod = Optional.ofNullable(getTransport())
                .map(method -> IngressTransportMethod.fromString(method.getValue())).orElse(null);
        return new Ingress()
                .withAllowInsecure(isAllowInsecure())
                .withExternal(isExternal())
                .withTargetPort(getTargetPort())
                .withTraffic(trafficList)
                .withTransport(transportMethod);
    }
}
