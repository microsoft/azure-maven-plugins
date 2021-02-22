/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.auth;

import com.google.common.base.Preconditions;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureLoginException;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCredentialWrapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class AzureClientFactory {
    public static Azure getAzureClient(AzureCredentialWrapper azureTokenCredentials,
                                       String userAgent, String defaultSubscriptionId) throws IOException, AzureLoginException {
        Preconditions.checkNotNull(azureTokenCredentials, "The parameter 'azureTokenCredentials' cannot be null.");
        final Authenticated authenticated = Azure.configure().withUserAgent(userAgent)
                .authenticate(azureTokenCredentials.getAzureTokenCredentials());

        return StringUtils.isEmpty(defaultSubscriptionId) ? authenticated.withDefaultSubscription() :
                authenticated.withSubscription(defaultSubscriptionId);
    }
}
