/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.tools.auth.model;

import com.azure.core.credential.TokenCredential;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.tools.auth.AzureIdentityCredentialTokenCredentials;
import com.microsoft.azure.tools.common.util.TextUtils;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

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
    private String tenantId;


    public AzureCredentialWrapper(AuthMethod method, TokenCredential tokenCredential, AzureEnvironment env) {
        Objects.requireNonNull(method, "authMethod is null");
        Objects.requireNonNull(tokenCredential, "tokenCredential is null");
        Objects.requireNonNull(tokenCredential, "env is null");
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

    public String getCredentialDescription() {
        List<String> details = new ArrayList<>();
        details.add(String.format("auth type: %s", TextUtils.green(authMethod.toString())));
        if (StringUtils.isNotBlank(tenantId)) {
            details.add(String.format("tenantId: %s", TextUtils.green(tenantId)));
        }
        if (StringUtils.isNotBlank(defaultSubscriptionId)) {
            details.add(String.format("default subscription: %s", TextUtils.green(defaultSubscriptionId)));
        }

        return StringUtils.join(details.toArray(), "\n");
    }

    /**
     * Get <class>AzureTokenCredentials</class> instance from track2 credential
     * @return AzureTokenCredentials
     */
    public AzureTokenCredentials getAzureTokenCredentials() {
        return new AzureIdentityCredentialTokenCredentials(env, tenantId, tokenCredential);
    }
}
