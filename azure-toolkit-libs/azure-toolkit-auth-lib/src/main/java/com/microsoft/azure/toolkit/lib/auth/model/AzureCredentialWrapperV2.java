/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import com.azure.core.credential.TokenCredential;
import com.google.common.base.Preconditions;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.auth.AzureIdentityCredentialTokenCredentials;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class AzureCredentialWrapperV2 {
    @Getter
    private AuthMethod authMethod;

    @Getter
    private TokenCredential tokenCredential;

    @Getter
    private AzureEnvironment env;

    @Getter
    private String tenantId;

    public AzureCredentialWrapperV2(AuthMethod method, AzureEnvironment env, String tenantId, TokenCredential tokenCredential) {
        Objects.requireNonNull(method, "authMethod is null");
        Objects.requireNonNull(tokenCredential, "tokenCredential is null");
        Objects.requireNonNull(env, "env is null");
        Preconditions.checkArgument(StringUtils.isNotBlank(tenantId), "tenantId is null or empty");
        this.authMethod = method;
        this.tokenCredential = tokenCredential;
        this.env = env;
        this.tenantId = tenantId;
    }

    public AzureTokenCredentials getAzureTokenCredentials() {
        return new AzureIdentityCredentialTokenCredentials(env, tenantId, tokenCredential);
    }
}
