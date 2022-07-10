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
import lombok.Getter;
import me.alexpanov.net.FreePortFinder;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Optional;

public class OAuthAccount extends Account {
    @Getter
    private final AuthType type = AuthType.OAUTH2;

    public OAuthAccount() {
        this(new AuthConfiguration(AuthType.OAUTH2));
    }

    public OAuthAccount(@Nonnull AuthConfiguration config) {
        super(config);
    }

    @Override
    public boolean checkAvailable() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }

    @Nonnull
    @Override
    protected TokenCredential buildDefaultTokenCredential() {
        final String tenantId = Optional.of(this.getConfig()).map(AuthConfiguration::getTenant).orElse(null);
        return new InteractiveBrowserCredentialBuilder()
            .clientId(this.getClientId())
            .tenantId(tenantId)
            .tokenCachePersistenceOptions(getPersistenceOptions())
            .redirectUrl("http://localhost:" + FreePortFinder.findFreeLocalPort())
            .executorService(this.getConfig().getExecutorService())
            .build();
    }
}
