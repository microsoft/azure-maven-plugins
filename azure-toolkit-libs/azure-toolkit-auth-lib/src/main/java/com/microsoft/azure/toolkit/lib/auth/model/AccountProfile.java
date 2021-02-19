/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import com.microsoft.azure.toolkit.lib.auth.core.ICredentialBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class AccountProfile {
    @Setter
    @Getter
    private AuthMethod method;

    @Setter
    @Getter
    private String environment;

    @Setter
    @Getter
    private String clientId;

    @Setter
    @Getter
    private List<String> tenantIds;

    @Setter
    @Getter
    private List<String> selectedSubscriptionIds;

    @Setter
    @Getter
    private String email;

    @Setter
    @Getter
    private boolean authenticated;

    @Setter
    @Getter
    private ICredentialBuilder credentialBuilder;

    @Setter
    @Getter
    private Throwable error;
}
