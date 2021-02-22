/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.oauth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.util.IdentityConstants;
import com.google.common.base.MoreObjects;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.refreshtoken.RefreshTokenCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.SubscriptionEntity;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentV2Utils;
import me.alexpanov.net.FreePortFinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.Objects;

public class OAuthAccountEntityBuilder implements IAccountEntityBuilder {
    private AzureEnvironment environment;

    public OAuthAccountEntityBuilder(AzureEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public AccountEntity build() {
        AccountEntity profile = new AccountEntity();
        profile.setMethod(AuthMethod.OAUTH2);
        profile.setAuthenticated(false);
        AzureEnvironment env = MoreObjects.firstNonNull(this.environment, AzureEnvironment.AZURE);
        profile.setEnvironment(AzureEnvironmentV2Utils.getCloudNameForAzureCli(env));
        int port = FreePortFinder.findFreeLocalPort();
        try {
            final TokenRequestContext request = new TokenRequestContext().addScopes(env.getManagementEndpoint() + "/.default");
            MsalToken msalToken = new OAuthCredential(env, IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID)
                    .authenticateWithBrowserInteraction(request, port).block();
            IAuthenticationResult result = msalToken.getAuthenticationResult();
            if (result != null && result.account() != null) {
                profile.setEmail(result.account().username());
            }
            String refreshToken = (String) FieldUtils.readField(result, "refreshToken", true);
            if (StringUtils.isBlank(refreshToken)) {
                throw new LoginFailureException("Cannot get refresh token from oauth2 workflow.");
            }

            profile.setCredentialBuilder(new ICredentialBuilder() {
                @Override
                public TokenCredential getCredentialWrapperForSubscription(SubscriptionEntity subscriptionEntity) {
                    Objects.requireNonNull(subscriptionEntity, "Parameter 'subscriptionEntity' cannot be null for building credentials.");
                    return new RefreshTokenCredentialBuilder().buildTokenCredential(env, subscriptionEntity.getTenantId(), refreshToken);
                }

                @Override
                public TokenCredential getCredentialForTenant(String tenantId) {
                    return new RefreshTokenCredentialBuilder().buildTokenCredential(env, tenantId, refreshToken);
                }

                @Override
                public TokenCredential getCredentialForListingTenants() {
                    return new RefreshTokenCredentialBuilder().buildTokenCredential(env, null, refreshToken);
                }
            });
            profile.setAuthenticated(true);
        } catch (Throwable ex) {
            profile.setAuthenticated(false);
            profile.setError(ex);
        }
        return profile;
    }
}
