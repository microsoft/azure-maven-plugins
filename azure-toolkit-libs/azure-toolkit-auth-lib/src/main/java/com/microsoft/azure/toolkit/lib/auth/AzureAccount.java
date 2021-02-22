/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.azurecli.AzureCliAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.maven.MavenLoginAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.oauth.OAuthAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.vscode.VisualStudioCodeAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureAccount implements AzureService {

    private Account account;

    @Setter
    @Getter
    private AzureEnvironment environment;

    public List<AccountEntity> getAvailableAccounts() {
        Map<AuthType, IAccountEntityBuilder> allBuilders = buildProfilerBuilders(environment);
        return allBuilders.values().stream().map(builder -> builder.build()).collect(Collectors.toList());
    }

    public void login(AuthConfiguration auth) {
        environment = auth.getEnvironment();
        Map<AuthType, IAccountEntityBuilder> allBuilders = buildProfilerBuilders(environment);
        if (allBuilders.containsKey(auth.getType())) {
            loginByBuilder(allBuilders.get(auth.getType()));
            return;
        }

        for (IAccountEntityBuilder builder : allBuilders.values()) {
            if (loginByBuilder(builder)) {
                return;
            }
        }
    }

    /**
     * @return account if authenticated
     * @throws AzureToolkitAuthenticationException if failed to authenticate
     */
    public Account login(AccountEntity accountEntity) throws AzureToolkitAuthenticationException {
        Objects.requireNonNull(accountEntity, "accountEntity is required.");
        this.account = new Account();
        final AccountEntity entity = new AccountEntity();
        entity.setMethod(accountEntity.getMethod());
        entity.setAuthenticated(accountEntity.isAuthenticated());
        entity.setEnvironment(accountEntity.getEnvironment());
        entity.setEmail(accountEntity.getEmail());
        entity.setTenantIds(accountEntity.getTenantIds());
        this.account.setEntity(entity);
        this.account.setCredentialBuilder(accountEntity.getCredentialBuilder());
        if (!accountEntity.isAuthenticated()) {
            return this.account;
        }
        // get
        this.account.initialize();
        this.account.selectSubscriptions(accountEntity.getSelectedSubscriptionIds());
        return this.account;
    }

    /**
     * @return account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.account)
            .orElseThrow(() -> new AzureToolkitAuthenticationException("Account is not initialized."));
    }

    private static Map<AuthType, IAccountEntityBuilder> buildProfilerBuilders(AzureEnvironment env) {
        Map<AuthType, IAccountEntityBuilder> map = new LinkedHashMap<>();
        map.put(AuthType.AZURE_CLI, new AzureCliAccountEntityBuilder());
        map.put(AuthType.AZURE_AUTH_MAVEN_PLUGIN, new MavenLoginAccountEntityBuilder());
        map.put(AuthType.VSCODE, new VisualStudioCodeAccountEntityBuilder());
        map.put(AuthType.OAUTH2, new OAuthAccountEntityBuilder(env));
        return map;
    }

    private boolean loginByBuilder(IAccountEntityBuilder builder) {
        AccountEntity profile = builder.build();
        if (profile != null && profile.isAuthenticated()) {
            Azure.az(AzureAccount.class).login(profile);
        }
        return Azure.az(AzureAccount.class).account().isAuthenticated();
    }
}
