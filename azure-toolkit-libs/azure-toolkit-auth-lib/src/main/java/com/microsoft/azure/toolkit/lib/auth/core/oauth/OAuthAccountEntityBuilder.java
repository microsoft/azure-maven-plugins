/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.oauth;

import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.util.IdentityConstants;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.util.AccountBuilderUtils;
import com.microsoft.azure.toolkit.lib.auth.core.common.MsalTokenBuilder;
import com.microsoft.azure.toolkit.lib.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import lombok.AllArgsConstructor;
import me.alexpanov.net.FreePortFinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.awt.*;

@AllArgsConstructor
public class OAuthAccountEntityBuilder implements IAccountEntityBuilder {
    private static final String CLI_CLIENT_ID = IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID;
    private AzureEnvironment environment;

    @Override
    public AccountEntity build() {
        AccountEntity accountEntity = AccountBuilderUtils.createAccountEntity(AuthMethod.OAUTH2);
        accountEntity.setEnvironment(this.environment);

        try {
            if (!isBrowserAvailable()) {
                throw new DesktopNotSupportedException("Not able to launch a browser to log you in.");
            }
            int port = FreePortFinder.findFreeLocalPort();
            MsalToken msalToken = new MsalTokenBuilder(this.environment, IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID)
                    .buildWithBrowserInteraction(port).block();
            IAuthenticationResult result = msalToken.getAuthenticationResult();
            if (result != null && result.account() != null) {
                accountEntity.setEmail(result.account().username());
            }
            String refreshToken = (String) FieldUtils.readField(result, "refreshToken", true);
            if (StringUtils.isBlank(refreshToken)) {
                throw new LoginFailureException("Cannot get refresh token from oauth2 workflow.");
            }
            AccountBuilderUtils.setRefreshCredentialBuilder(accountEntity, CLI_CLIENT_ID, refreshToken);
            accountEntity.setAuthenticated(true);
        } catch (Throwable ex) {
            accountEntity.setAuthenticated(false);
            accountEntity.setError(ex);
        }
        return accountEntity;
    }

    private static boolean isBrowserAvailable() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
