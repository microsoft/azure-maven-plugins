/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.oauth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.InteractiveBrowserCredential;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.util.IdentityConstants;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.azure.toolkit.lib.auth.MasterTokenCredential;
import com.microsoft.azure.toolkit.lib.auth.core.refresktoken.RefreshTokenMasterTokenCredential;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.auth.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.alexpanov.net.FreePortFinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.awt.*;

@AllArgsConstructor
public class OAuthAccount extends Account {
    @Getter
    private final AuthMethod method = AuthMethod.OAUTH2;

    private AzureEnvironment environment;

    @Override
    public boolean isAvailable() {
        return isBrowserAvailable();
    }

    @Override
    public void initializeCredentials() throws LoginFailureException {
        if (!isBrowserAvailable()) {
            throw new LoginFailureException("Not able to launch a browser to log you in.");
        }

        AzureEnvironmentUtils.setupAzureEnvironment(environment);
        InteractiveBrowserCredential interactiveBrowserCredential = new InteractiveBrowserCredentialBuilder()
                .redirectUrl("http://localhost:" + FreePortFinder.findFreeLocalPort())
                .build();
        AccessToken accessToken = interactiveBrowserCredential.getToken(new TokenRequestContext()
                .addScopes(ScopeUtil.resourceToScopes(environment.getManagementEndpoint()))).block();

        // legacy code will be removed after https://github.com/jongio/azidext/pull/41 is merged
        IAuthenticationResult result = ((MsalToken) accessToken).getAuthenticationResult();
        if (result != null && result.account() != null) {
            entity.setEmail(result.account().username());
        }
        String refreshToken;
        try {
            refreshToken = (String) FieldUtils.readField(result, "refreshToken", true);
        } catch (IllegalAccessException e) {
            throw new LoginFailureException("Cannot read refreshToken from InteractiveBrowserCredential.");
        }
        if (StringUtils.isBlank(refreshToken)) {
            throw new LoginFailureException("Cannot get refresh token from oauth2 workflow.");
        }

        MasterTokenCredential oauthMasterTokenCredential =
                new RefreshTokenMasterTokenCredential(environment, IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID, refreshToken);
        entity.setCredential(oauthMasterTokenCredential);
    }

    private static boolean isBrowserAvailable() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
