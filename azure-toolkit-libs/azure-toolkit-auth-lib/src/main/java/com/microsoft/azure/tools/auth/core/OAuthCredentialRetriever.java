/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.azure.identity.InteractiveBrowserCredential;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import me.alexpanov.net.FreePortFinder;

import java.awt.*;

public class OAuthCredentialRetriever extends AbstractCredentialRetriever {
    private static final String AZURE_TOOLKIT_CLIENT_ID = "777acee8-5286-4d6e-8b05-f7c851d8ed0a";
    private AzureEnvironment env;

    public OAuthCredentialRetriever(AzureEnvironment env) {
        this.env = env;
    }

    public AzureCredentialWrapper retrieve() throws LoginFailureException {
        if (!isBrowserAvailable()) {
            throw new DesktopNotSupportedException("Not able to launch a browser to log you in.");
        }

        InteractiveBrowserCredential interactiveBrowserCredential = new InteractiveBrowserCredentialBuilder()
                .clientId(AZURE_TOOLKIT_CLIENT_ID).redirectUrl("http://localhost:" + FreePortFinder.findFreeLocalPort())
                .build();
        return new AzureCredentialWrapper(AuthMethod.OAUTH2, interactiveBrowserCredential, env);
    }

    private static boolean isBrowserAvailable() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
