/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.oauth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.InteractiveBrowserCredential;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import me.alexpanov.net.FreePortFinder;

import java.awt.*;

public class OAuthAccount extends Account {
    private final AuthMethod method = AuthMethod.OAUTH2;

    @Override
    public AuthMethod getMethod() {
        return method;
    }

    @Override
    protected boolean checkAvailableInner() {
        return isBrowserAvailable();
    }

    @Override
    protected TokenCredential createTokenCredential() {
        AzureEnvironment env = Azure.az(AzureCloud.class).getOrDefault();
        this.entity.setEnvironment(env);
        AzureEnvironmentUtils.setupAzureEnvironment(env);
        InteractiveBrowserCredential interactiveBrowserCredential = new InteractiveBrowserCredentialBuilder()
                .redirectUrl("http://localhost:" + FreePortFinder.findFreeLocalPort())
                .build();
        return interactiveBrowserCredential;
    }

    private static boolean isBrowserAvailable() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
