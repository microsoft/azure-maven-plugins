/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.SingleTenantCredential;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.ValidationUtil;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ServicePrincipalAccount extends Account {
    @Getter
    private final AuthMethod method = AuthMethod.SERVICE_PRINCIPAL;
    private AuthConfiguration configuration;
    private TokenCredential clientSecretCredential;

    public ServicePrincipalAccount(@Nonnull AuthConfiguration authConfiguration) {
        Objects.requireNonNull(authConfiguration);
        this.configuration = authConfiguration;
    }

    @Override
    protected boolean checkAvailableInner() {
        try {
            ValidationUtil.validateAuthConfiguration(configuration);
            return true;
        } catch (InvalidConfigurationException e) {
            throw new AzureToolkitAuthenticationException(
                    "Cannot login through 'SERVICE_PRINCIPAL' due to invalid configuration:" + e.getMessage());
        }
    }

    @Override
    protected void initializeCredentials() throws LoginFailureException {
        this.entity.setEnvironment(ObjectUtils.firstNonNull(configuration.getEnvironment(), AzureEnvironment.AZURE));
        verifyTokenCredential(ObjectUtils.firstNonNull(configuration.getEnvironment(), AzureEnvironment.AZURE), clientSecretCredential);
        this.entity.setTenantCredential(new SingleTenantCredential(clientSecretCredential));
    }
}
