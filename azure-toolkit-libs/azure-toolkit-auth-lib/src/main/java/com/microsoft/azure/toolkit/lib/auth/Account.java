/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialProvider;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.SubscriptionEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

public class Account {
    @Getter
    @Setter
    private AccountEntity entity;

    @Setter
    @Getter
    private ICredentialProvider credentialBuilder;

    private Map<String, TokenCredential> tenantToCredential = new HashMap<>();

    public Account logout() {
        this.entity = null;
        this.credentialBuilder = null;
        this.tenantToCredential = new HashMap<>();
        return this;
    }

    public AzureEnvironment getEnvironment() {
        return entity.getEnvironment();
    }

    public void fillTenantAndSubscriptions() {
        Objects.requireNonNull(entity, "Cannot initialize from null account entity.");
    }

    public boolean isAuthenticated() {
        return this.entity != null && this.entity.isAuthenticated();
    }

    public List<SubscriptionEntity> getSubscriptions() {
        if (this.entity != null) {
            return this.entity.getSubscriptions();
        }
        return null;
    }

    public List<SubscriptionEntity> getSelectedSubscriptions() {
        if (this.entity != null) {
            return this.entity.getSelectedSubscriptions();
        }
        return null;
    }

    public TokenCredential getCredential(String subscriptionId) throws LoginFailureException {
        return null;
    }

    public AzureTokenCredentials getCredentialV1(String subscriptionId) throws LoginFailureException {
        return null;
    }
}
