/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.oauth;

import com.azure.core.management.AzureEnvironment;
import com.azure.identity.InteractiveBrowserCredential;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.refresktoken.RefreshTokenAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import lombok.Getter;
import me.alexpanov.net.FreePortFinder;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.awt.*;

public class OAuthAccount extends RefreshTokenAccount {
    @Getter
    private final AuthMethod method = AuthMethod.OAUTH2;

    public OAuthAccount(@Nonnull AzureEnvironment environment) {
        this.environment = environment;
    }

    @Override
    protected void initializeRefreshToken() {
        // empty since the refresh token is not available now
    }

    @Override
    protected Mono<Boolean> checkAvailableInner() {
        return Mono.just(isBrowserAvailable());
    }

    @Override
    protected void initializeCredentials() throws LoginFailureException {
        AzureEnvironmentUtils.setupAzureEnvironment(environment);
        InteractiveBrowserCredential interactiveBrowserCredential = new InteractiveBrowserCredentialBuilder()
                .redirectUrl("http://localhost:" + FreePortFinder.findFreeLocalPort())
                .build();
        initializeFromTokenCredential(interactiveBrowserCredential);
    }

    private static boolean isBrowserAvailable() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
