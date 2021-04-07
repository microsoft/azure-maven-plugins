/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.vscode;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.ObjectUtils;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.core.refresktoken.RefreshTokenTenantCredential;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VisualStudioCodeAccount extends Account {
    private static final String VSCODE_CLIENT_ID = "aebc6443-996d-45c2-90f0-388ff96faa56";

    private String refreshToken;
    private String vscodeCloudName;
    private Map<String, String> vscodeUserSettings;

    @Override
    public AuthMethod getMethod() {
        return AuthMethod.VSCODE;
    }

    @Override
    protected boolean checkAvailableInner() {
        VisualStudioCacheAccessor accessor = new VisualStudioCacheAccessor();
        vscodeUserSettings = accessor.getUserSettingsDetails();
        vscodeCloudName = vscodeUserSettings.get("cloud");
        refreshToken = accessor.getCredentials("VS Code Azure", vscodeCloudName);
        return StringUtils.isNotBlank(refreshToken);
    }

    @Override
    protected TokenCredential createTokenCredential() {
        if (isAvailable() || checkAvailableInner()) {
            AzureEnvironment environment = ObjectUtils.firstNonNull(AzureEnvironmentUtils.stringToAzureEnvironment(vscodeCloudName), AzureEnvironment.AZURE);
            this.entity.setEnvironment(environment);
            if (vscodeUserSettings.containsKey("filter")) {
                final List<String> filteredSubscriptions = Arrays.asList(StringUtils.split(vscodeUserSettings.get("filter"), ","));
                this.entity.setSelectedSubscriptionIds(filteredSubscriptions);
            }
            String authority = AzureEnvironmentUtils.getAuthority(environment);
            final RefreshTokenTenantCredential tenantCredential = new RefreshTokenTenantCredential(authority, VSCODE_CLIENT_ID, refreshToken);
            this.entity.setTenantCredential(tenantCredential);
            return tenantCredential;
        }
        throw new AzureToolkitAuthenticationException("Cannot get credential from vscode.");
    }
}
