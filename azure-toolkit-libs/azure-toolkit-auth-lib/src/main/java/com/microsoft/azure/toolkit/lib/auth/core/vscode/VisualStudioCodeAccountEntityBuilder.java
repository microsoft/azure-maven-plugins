/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.vscode;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.refreshtoken.RefreshTokenCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.SubscriptionEntity;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentV2Utils;
import com.microsoft.azure.toolkit.lib.auth.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VisualStudioCodeAccountEntityBuilder implements IAccountEntityBuilder {
    @Override
    public AccountEntity build() {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setMethod(AuthMethod.VSCODE);
        accountEntity.setAuthenticated(false);
        VisualStudioCacheAccessor accessor = new VisualStudioCacheAccessor();
        try {
            Map<String, String> userSettings = accessor.getUserSettingsDetails();
            String vscodeCloudName = userSettings.get("cloud");
            List<String> filteredSubscriptions;
            if (userSettings.containsKey("filter")) {
                filteredSubscriptions = Arrays.asList(StringUtils.split(userSettings.get("filter"), ","));
            } else {
                filteredSubscriptions = new ArrayList<>();
            }

            accountEntity.setEnvironment(vscodeCloudName);

            String refreshToken = accessor.getCredentials("VS Code Azure", vscodeCloudName);
            if (StringUtils.isEmpty(refreshToken)) {
                throw new LoginFailureException("Cannot get credentials from VSCode, please make sure that you have signed-in in VSCode Azure Account plugin");
            }
            accountEntity.setSelectedSubscriptionIds(filteredSubscriptions);
            AzureEnvironment env = Utils.firstNonNull(AzureEnvironmentV2Utils.stringToAzureEnvironment(vscodeCloudName), AzureEnvironment.AZURE);
            accountEntity.setCredentialBuilder(new ICredentialBuilder() {
                @Override
                public TokenCredential getCredentialWrapperForSubscription(SubscriptionEntity subscriptionEntity) {
                    Objects.requireNonNull(subscriptionEntity, "Parameter 'subscriptionEntity' cannot be null for building credentials.");
                    return new RefreshTokenCredentialBuilder().buildVSCodeTokenCredential(env, subscriptionEntity.getTenantId(), refreshToken);
                }

                @Override
                public TokenCredential getCredentialForTenant(String tenantId) {
                    return new RefreshTokenCredentialBuilder().buildVSCodeTokenCredential(env, tenantId, refreshToken);
                }

                @Override
                public TokenCredential getCredentialForListingTenants() {
                    return new RefreshTokenCredentialBuilder().buildVSCodeTokenCredential(env, null, refreshToken);
                }
            });
            accountEntity.setAuthenticated(true);
        } catch (LoginFailureException ex) {
            accountEntity.setError(ex);
            accountEntity.setAuthenticated(false);
        }

        return accountEntity;
    }
}
