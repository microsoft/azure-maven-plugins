/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.oauth;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import me.alexpanov.net.FreePortFinder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Optional;

public class OAuthAccount extends Account {
    private final AuthConfiguration config;

    public OAuthAccount() {
        this(null);
    }

    public OAuthAccount(@Nullable AuthConfiguration config) {
        super(AuthType.OAUTH2);
        this.config = config;
    }

    @Override
    public boolean checkAvailable() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }

    @Nonnull
    @Override
    protected TokenCredential buildDefaultTokenCredential() {
        final String tenantId = Optional.ofNullable(this.config).map(AuthConfiguration::getTenant).orElse(null);
        return new InteractiveBrowserCredentialBuilder()
            .clientId(this.getClientId())
            .tenantId(tenantId)
            .tokenCachePersistenceOptions(getPersistenceOptions())
            .redirectUrl("http://localhost:" + FreePortFinder.findFreeLocalPort())
            .build();
    }
}
