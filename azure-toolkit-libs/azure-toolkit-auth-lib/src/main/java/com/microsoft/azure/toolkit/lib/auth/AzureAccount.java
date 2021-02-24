/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.util.logging.ClientLogger;
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
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.auth.util.ValidationUtil;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
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
        return allBuilders.values().stream().map(IAccountEntityBuilder::build).collect(Collectors.toList());
    }

    public void login(AuthConfiguration auth) throws LoginFailureException {
        // update the env state of AzureAccount when auth configuration has a strong configuration of env
        this.environment = Utils.firstNonNull(auth.getEnvironment(), this.environment);
        Objects.requireNonNull(auth, "Null 'auth' cannot be used to sign-in.");
        AzureEnvironmentUtils.setupAzureEnvironment(auth.getEnvironment());
        loginInner(auth);
        if (auth.getEnvironment() != null && this.account.getEnvironment() != null
                && this.account.getEnvironment() != this.environment
                && this.account.isAuthenticated()) {

            String expectedEnv = AzureEnvironmentUtils.getCloudNameForAzureCli(auth.getEnvironment());
            String realEnv = AzureEnvironmentUtils.getCloudNameForAzureCli(this.account.getEnvironment());

            // conflicting configuration of azure environment
            switch (this.account.getEntity().getMethod()) {
                case AZURE_CLI:
                    throw new LoginFailureException(String.format("The azure cloud from azure cli '%s' doesn't match with your auth configuration, " +
                                    "you can change it by executing 'az cloud set --name=%s' command to change the cloud in azure cli.",
                            realEnv,
                            expectedEnv));

                case AZURE_SECRET_FILE:
                    throw new LoginFailureException(String.format("The azure cloud from maven login '%s' doesn't match with your auth configuration, " +
                                    "please switch to other auth method for '%s' environment.",
                            realEnv,
                            expectedEnv));
                case VSCODE:
                    throw new LoginFailureException(String.format("The azure cloud from vscode '%s' doesn't match with your auth configuration: %s, " +
                                    "you can change it by pressing F1 in VSCode and find \">azure: sign in to Azure Cloud\" command " +
                                    "to change azure cloud in vscode.",
                            realEnv,
                            expectedEnv));
                default:
                    // empty
            }
        }
    }

    public void login(AccountEntity accountEntity) throws AzureToolkitAuthenticationException {
        Objects.requireNonNull(accountEntity, "accountEntity is required.");
        this.account = new Account();
        final AccountEntity entity = new AccountEntity();
        try {
            Utils.copyProperties(entity, accountEntity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AzureToolkitAuthenticationException(
                    String.format("Cannot copy properties on class AccountEntity due to error: %s", e.getMessage()));
        }

        this.account.setEntity(entity);
        this.account.setCredentialBuilder(accountEntity.getCredentialBuilder());
        if (!accountEntity.isAuthenticated()) {
            return;
        }
        // get
        this.account.fillTenantAndSubscriptions();
    }

    /**
     * @return the current account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.account)
            .orElseThrow(() -> new AzureToolkitAuthenticationException("Account is not initialized."));
    }

    private void loginInner(AuthConfiguration auth) throws LoginFailureException {
        if (auth.getType() == AuthType.SERVICE_PRINCIPAL || auth.getType() == AuthType.AUTO) {
            if (loginServicePrincipal(auth)) {
                return;
            }
        }
        Map<AuthType, IAccountEntityBuilder> allBuilders = buildProfilerBuilders(environment);
        if (allBuilders.containsKey(auth.getType())) {
            loginByBuilder(allBuilders.get(auth.getType()));
        } else {
            for (IAccountEntityBuilder builder : allBuilders.values()) {
                if (loginByBuilder(builder)) {
                    break;
                }
            }
        }
    }

    private boolean loginServicePrincipal(AuthConfiguration auth) throws LoginFailureException {
        boolean forceServicePrincipalLogin = auth.getType() == AuthType.SERVICE_PRINCIPAL;
        boolean isSPConfigurationPresent = !StringUtils.isAllBlank(auth.getCertificate(), auth.getKey(),
                auth.getCertificatePassword());
        try {
            ValidationUtil.validateAuthConfiguration(auth);
            AzureEnvironmentUtils.setupAzureEnvironment(auth.getEnvironment());
            ServicePrincipalAccountEntityBuilder builder = new ServicePrincipalAccountEntityBuilder(auth);
            AccountEntity entity = builder.build();
            if (entity.isAuthenticated()) {
                login(entity);
            }
            return entity.isAuthenticated();
        } catch (InvalidConfigurationException e) {
            if (forceServicePrincipalLogin) {
                throw new LoginFailureException(e.getMessage(), e);
            }
            if (isSPConfigurationPresent) {
                logger.warning("Cannot login through 'SERVICE_PRINCIPAL' due to invalid configuration: " + e.getMessage());
            }
        }
        return false;
    }

    private static Map<AuthType, IAccountEntityBuilder> buildProfilerBuilders(AzureEnvironment env) {
        Map<AuthType, IAccountEntityBuilder> map = new LinkedHashMap<>();
        // SP is not there since it requires special constructor argument and it is handled in login(AuthConfiguration auth)
        AzureEnvironment environmentOrDefault = Utils.firstNonNull(env, AzureEnvironment.AZURE);
        map.put(AuthType.MANAGED_IDENTITY, new ManagedIdentityAccountEntityBuilder(environmentOrDefault));
        map.put(AuthType.AZURE_CLI, new AzureCliAccountEntityBuilder());

        map.put(AuthType.VSCODE, new VisualStudioCodeAccountEntityBuilder());
        map.put(AuthType.VISUAL_STUDIO, new VisualStudioAccountEntityBuilder());
        map.put(AuthType.AZURE_AUTH_MAVEN_PLUGIN, new MavenLoginAccountEntityBuilder());
        map.put(AuthType.OAUTH2, new OAuthAccountEntityBuilder(environmentOrDefault));
        map.put(AuthType.DEVICE_CODE, new DeviceCodeAccountEntityBuilder(environmentOrDefault));
        return map;
    }

    private boolean loginByBuilder(IAccountEntityBuilder builder) {
        AccountEntity profile = builder.build();
        if (profile != null && profile.isAuthenticated()) {
            Azure.az(AzureAccount.class).login(profile);
        }
        return profile.isAuthenticated();
    }
}
