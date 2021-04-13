/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.oauth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.azure.identity.implementation.util.IdentityConstants;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.TokenCredentialManager;
import com.microsoft.azure.toolkit.lib.auth.RefreshTokenTokenCredentialManager;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import me.alexpanov.net.FreePortFinder;
import reactor.core.publisher.Mono;

import java.awt.*;

public class OAuthAccount extends Account {
    @Override
    public AuthType getAuthType() {
        return AuthType.OAUTH2;
    }

    @Override
    public String getClientId() {
        return IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID;
    }

    public Mono<Boolean> checkAvailable() {
        return preLoginCheck();
    }

    @Override
    protected Mono<Boolean> preLoginCheck() {
        return Mono.fromCallable(() -> {
            if (!isBrowserAvailable()) {
                throw new AzureToolkitAuthenticationException("Browser is not available for oauth login.");
            }
            return true;
        });
    }

    protected TokenCredential createCredential() {
        AzureEnvironmentUtils.setupAzureEnvironment(this.entity.getEnvironment());
        return new InteractiveBrowserCredentialBuilder()
                .redirectUrl("http://localhost:" + FreePortFinder.findFreeLocalPort())
                .build();
    }

    protected Mono<TokenCredentialManager> createTokenCredentialManager() {
        AzureEnvironment env = Azure.az(AzureCloud.class).getOrDefault();
        this.entity.setEnvironment(env);
        return RefreshTokenTokenCredentialManager.createTokenCredentialManager(env, getClientId(), createCredential());
    }

    private static boolean isBrowserAvailable() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
