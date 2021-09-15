/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder()
@EqualsAndHashCode()
public class SecurityRule {
    public static final SecurityRule SSH_RULE = SecurityRule.builder().name("SSH")
            .toPort(22).protocol(Protocol.TCP).build();
    public static final SecurityRule HTTP_RULE = SecurityRule.builder().name("HTTP")
            .toPort(80).protocol(Protocol.TCP).build();
    public static final SecurityRule HTTPS_RULE = SecurityRule.builder().name("HTTPS")
            .toPort(443).protocol(Protocol.TCP).build();
    public static final SecurityRule RDP_RULE = SecurityRule.builder().name("RDP")
            .toPort(3389).protocol(Protocol.TCP).build();

    private String name;
    private String[] fromAddresses;
    private Integer fromPort;
    private String[] toAddresses;
    private Integer toPort;
    private Protocol protocol;

    @AllArgsConstructor
    public enum Protocol {
        TCP("Tcp"),
        UDP("Udp"),
        ICMP("Icmp"),
        ESP("Esp"),
        ASTERISK("*"),
        AH("Ah"),
        ALL("all");

        private String value;
    }
}
