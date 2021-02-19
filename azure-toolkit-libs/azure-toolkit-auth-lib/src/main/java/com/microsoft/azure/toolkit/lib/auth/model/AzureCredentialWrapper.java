/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.auth.model;

import com.azure.core.credential.TokenCredential;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.auth.AzureIdentityCredentialTokenCredentials;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AzureCredentialWrapper {
    @Getter
    private AuthMethod authMethod;

    @Getter
    private TokenCredential tokenCredential;

    @Getter
    private AzureEnvironment env;

    @Getter
    private String defaultSubscriptionId;

    @Getter
    private String[] filteredSubscriptionIds;

    @Getter
    private String tenantId;


    public AzureCredentialWrapper(AuthMethod method, TokenCredential tokenCredential, AzureEnvironment env) {
        Objects.requireNonNull(method, "authMethod is null");
        Objects.requireNonNull(tokenCredential, "tokenCredential is null");
        Objects.requireNonNull(env, "env is null");
        this.authMethod = method;
        this.tokenCredential = tokenCredential;
        this.env = env;
    }

    public AzureCredentialWrapper withDefaultSubscriptionId(String defaultSubscriptionId) {
        this.defaultSubscriptionId = defaultSubscriptionId;
        return this;
    }

    public AzureCredentialWrapper withTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public AzureCredentialWrapper withFilteredSubscriptionIds(String[] filteredSubscriptionIds) {
        this.filteredSubscriptionIds = filteredSubscriptionIds;
        return this;
    }

    public String getCredentialDescription() {
        List<String> details = new ArrayList<>();
        details.add(String.format("Auth method: %s", TextUtils.cyan(authMethod.toString())));
        if (StringUtils.isNotBlank(tenantId)) {
            details.add(String.format("Tenant id: %s", TextUtils.cyan(tenantId)));
        }
        if (StringUtils.isNotBlank(defaultSubscriptionId)) {
            details.add(String.format("Default subscription: %s", TextUtils.cyan(defaultSubscriptionId)));
        }

        return StringUtils.join(details.toArray(), "\n");
    }

    /**
     * Get <code>AzureTokenCredentials</code> instance from track2 credential
     *
     * @return AzureTokenCredentials
     */
    public AzureTokenCredentials getAzureTokenCredentials() {
        return new AzureIdentityCredentialTokenCredentials(env, tenantId, tokenCredential);
    }
}
