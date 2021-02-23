/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.util.logging.ClientLogger;
import com.google.common.base.MoreObjects;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.azurecli.AzureCliAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.devicecode.DeviceCodeAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.managedidentity.ManagedIdentityAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.maven.MavenLoginAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.oauth.OAuthAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal.ServicePrincipalAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.visualstudio.VisualStudioAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.vscode.VisualStudioCodeAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.ValidationUtil;
import lombok.Getter;
import lombok.Lombok;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureAccount implements AzureService {
    private final ClientLogger logger = new ClientLogger(AzureAccount.class);

    private Account account;

    @Setter
    @Getter
    private AzureEnvironment environment;

    public List<AccountEntity> getAvailableAccounts() {
        Map<AuthType, IAccountEntityBuilder> allBuilders = buildProfilerBuilders(environment);
        return allBuilders.values().stream().map(builder -> builder.build()).collect(Collectors.toList());
    }

    public void login(AuthConfiguration auth) throws LoginFailureException {
        environment = auth.getEnvironment();
        boolean forceSPLogin = auth.getType() == AuthType.SERVICE_PRINCIPAL;
        if (forceSPLogin || auth.getType() == AuthType.AUTO) {
            boolean isSPConfigurationPresent = !StringUtils.isAllBlank(auth.getCertificate(), auth.getKey(),
                    auth.getCertificatePassword());
            try {
                ValidationUtil.validateAuthConfiguration(auth);
            } catch (InvalidConfigurationException e) {
                if (forceSPLogin) {
                    Lombok.sneakyThrow(e);
                    return;
                }
                if (isSPConfigurationPresent) {
                    logger.warning("Cannot login through 'SERVICE_PRINCIPAL' due to invalid configuration: " + e.getMessage());
                }
            }

            ServicePrincipalAccountEntityBuilder builder = new ServicePrincipalAccountEntityBuilder(auth);
            AccountEntity entity = builder.build();
            if (entity.isAuthenticated()) {
                login(entity);
                return;
            }
        }
        Map<AuthType, IAccountEntityBuilder> allBuilders = buildProfilerBuilders(environment);
        if (allBuilders.containsKey(auth.getType())) {
            loginByBuilder(allBuilders.get(auth.getType()));
            return;
        } else {
            for (IAccountEntityBuilder builder : allBuilders.values()) {
                if (loginByBuilder(builder)) {
                    return;
                }
            }
        }


    }

    public void login(AccountEntity accountEntity) throws AzureToolkitAuthenticationException {
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
            return;
        }
        // get
        this.account.initialize();
        this.account.selectSubscriptions(accountEntity.getSelectedSubscriptionIds());
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
        // SP is not there since it requires special constructor argument and it is handled in login(AuthConfiguration auth)
        AzureEnvironment environmentOrDefault = MoreObjects.firstNonNull(env, AzureEnvironment.AZURE);
        map.put(AuthType.AZURE_CLI, new AzureCliAccountEntityBuilder());
        map.put(AuthType.AZURE_AUTH_MAVEN_PLUGIN, new MavenLoginAccountEntityBuilder());
        map.put(AuthType.VSCODE, new VisualStudioCodeAccountEntityBuilder());
        map.put(AuthType.OAUTH2, new OAuthAccountEntityBuilder(environmentOrDefault));
        map.put(AuthType.DEVICE_CODE, new DeviceCodeAccountEntityBuilder(environmentOrDefault));
        map.put(AuthType.MANAGED_IDENTITY, new ManagedIdentityAccountEntityBuilder(environmentOrDefault));
        map.put(AuthType.VISUAL_STUDIO, new VisualStudioAccountEntityBuilder());
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
