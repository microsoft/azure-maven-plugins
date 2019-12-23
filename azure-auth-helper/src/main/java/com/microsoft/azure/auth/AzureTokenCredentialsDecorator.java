/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.configuration.AuthType;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Proxy;

public class AzureTokenCredentialsDecorator extends AzureTokenCredentials {

    private AuthType authType;
    private AzureTokenCredentials azureTokenCredentials;

    public AzureTokenCredentialsDecorator(AuthType authType, AzureTokenCredentials credentials) {
        super(credentials.environment(), credentials.domain());
        this.authType = authType;
        this.azureTokenCredentials = credentials;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public AzureTokenCredentials getAzureTokenCredentials() {
        return azureTokenCredentials;
    }

    @Override
    public String getToken(String s) throws IOException {
        return azureTokenCredentials.getToken(s);
    }

    @Override
    public String domain() {
        return azureTokenCredentials.domain();
    }

    @Override
    public AzureEnvironment environment() {
        return azureTokenCredentials.environment();
    }

    @Override
    public String defaultSubscriptionId() {
        return azureTokenCredentials.defaultSubscriptionId();
    }

    @Override
    public AzureTokenCredentials withDefaultSubscriptionId(String subscriptionId) {
        return azureTokenCredentials.withDefaultSubscriptionId(subscriptionId);
    }

    @Override
    public Proxy proxy() {
        return azureTokenCredentials.proxy();
    }

    @Override
    public SSLSocketFactory sslSocketFactory() {
        return azureTokenCredentials.sslSocketFactory();
    }

    @Override
    public AzureTokenCredentials withProxy(Proxy proxy) {
        return azureTokenCredentials.withProxy(proxy);
    }

    @Override
    public AzureTokenCredentials withSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        return azureTokenCredentials.withSslSocketFactory(sslSocketFactory);
    }

    @Override
    public void applyCredentialsFilter(OkHttpClient.Builder clientBuilder) {
        azureTokenCredentials.applyCredentialsFilter(clientBuilder);
    }
}
