/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.vscode;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.common.CommonAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.auth.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VisualStudioCodeAccountEntityBuilder implements IAccountEntityBuilder {
    private static final String VSCODE_CLIENT_ID = "aebc6443-996d-45c2-90f0-388ff96faa56";

    @Override
    public AccountEntity build() {
        AccountEntity accountEntity = CommonAccountEntityBuilder.createAccountEntity(AuthMethod.VSCODE);
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
            AzureEnvironment env = Utils.firstNonNull(AzureEnvironmentUtils.stringToAzureEnvironment(vscodeCloudName), AzureEnvironment.AZURE);
            accountEntity.setEnvironment(env);

            String refreshToken = accessor.getCredentials("VS Code Azure", vscodeCloudName);
            if (StringUtils.isEmpty(refreshToken)) {
                throw new LoginFailureException("Cannot get credentials from VSCode, please make sure that you have signed-in in VSCode Azure Account plugin");
            }
            accountEntity.setSelectedSubscriptionIds(filteredSubscriptions);
            accountEntity.setCredentialBuilder(CommonAccountEntityBuilder.fromRefreshToken(env, VSCODE_CLIENT_ID, refreshToken));
            accountEntity.setAuthenticated(true);
        } catch (LoginFailureException ex) {
            accountEntity.setError(ex);
            accountEntity.setAuthenticated(false);
        }

        return accountEntity;
    }
}
