/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.oauth;

import com.azure.identity.InteractiveBrowserCredential;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.core.AbstractCredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.core.legacy.LegacyAsyncCredentialProxy;
import com.microsoft.azure.toolkit.lib.auth.core.legacy.LegacyRefreshTokenCredentialFactory;
import com.microsoft.azure.toolkit.lib.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCredentialWrapper;
import me.alexpanov.net.FreePortFinder;

import java.awt.*;

public class OAuthCredentialRetriever extends AbstractCredentialRetriever {
    public OAuthCredentialRetriever(AzureEnvironment env) {
        super(env);
    }

    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        if (!isBrowserAvailable()) {
            throw new DesktopNotSupportedException("Not able to launch a browser to log you in.");
        }
        InteractiveBrowserCredential interactiveBrowserCredential = new InteractiveBrowserCredentialBuilder()
                .redirectUrl("http://localhost:" + FreePortFinder.findFreeLocalPort())
                .build();

        return new AzureCredentialWrapper(AuthMethod.OAUTH2,
                new LegacyAsyncCredentialProxy(LegacyRefreshTokenCredentialFactory.createRefreshTokenCredential(
                        getAzureEnvironment(), interactiveBrowserCredential)),
                        getAzureEnvironment());
    }

    private static boolean isBrowserAvailable() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
