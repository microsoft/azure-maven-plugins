/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureLoginException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class AzureClientFactory {
    public static Azure getAzureClient(String userAgent, String defaultSubscriptionId) throws IOException, AzureLoginException {
        final Account account = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account();
        final Authenticated authenticated = Azure.configure().withUserAgent(userAgent)
                .authenticate(account.getTokenCredentialV1ForSubscription(defaultSubscriptionId));

        return StringUtils.isEmpty(defaultSubscriptionId) ? authenticated.withDefaultSubscription() :
                authenticated.withSubscription(defaultSubscriptionId);
    }
}
