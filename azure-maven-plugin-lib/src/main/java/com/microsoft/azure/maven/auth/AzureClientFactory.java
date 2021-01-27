/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.google.common.base.Preconditions;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.tools.auth.exception.AzureLoginException;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.tools.common.util.ProxyUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class AzureClientFactory {
    public static Azure getAzureClient(AzureCredentialWrapper azureTokenCredentials,
                                       String userAgent, String httpProxyHost, int httpProxyPort) throws IOException, AzureLoginException {
        Preconditions.checkNotNull(azureTokenCredentials, "The parameter 'azureTokenCredentials' cannot be null.");
        final String defaultSubscriptionId = azureTokenCredentials.getDefaultSubscriptionId();
        final Authenticated authenticated = Azure.configure().withUserAgent(userAgent)
                .withProxy(ProxyUtils.createHttpProxy(httpProxyHost, Integer.toString(httpProxyPort)))
                .authenticate(azureTokenCredentials.getAzureTokenCredentials());

        return StringUtils.isEmpty(defaultSubscriptionId) ? authenticated.withDefaultSubscription() :
                authenticated.withSubscription(defaultSubscriptionId);
    }
}
