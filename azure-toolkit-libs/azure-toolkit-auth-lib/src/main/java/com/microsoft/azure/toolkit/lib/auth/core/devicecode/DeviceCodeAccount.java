/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.devicecode;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.DeviceCodeInfo;
import com.azure.identity.TokenCachePersistenceOptions;
import com.azure.identity.implementation.util.IdentityConstants;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.Constant;
import com.microsoft.azure.toolkit.lib.auth.TokenCredentialManager;
import com.microsoft.azure.toolkit.lib.auth.RefreshTokenTokenCredentialManager;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DeviceCodeAccount extends Account {
    private static final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("azure-toolkit-auth-%d").build();

    private final ExecutorService executorService = createExecutorService();

    private final CompletableFuture<DeviceCodeInfo> deviceCodeFuture = new CompletableFuture<>();

    private Mono<Account> loginMono;

    @Override
    public AuthType getAuthType() {
        return AuthType.DEVICE_CODE;
    }

    @Override
    public String getClientId() {
        return IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID;
    }

    @Override
    protected Mono<Boolean> preLoginCheck() {
        return Mono.just(true);
    }

    public Mono<Boolean> checkAvailable() {
        return Mono.just(true);
    }

    protected Mono<TokenCredentialManager> createTokenCredentialManager() {
        AzureEnvironment env = Azure.az(AzureCloud.class).getOrDefault();
        return RefreshTokenTokenCredentialManager.createTokenCredentialManager(env, getClientId(), createCredential(env));
    }

    @Override
    protected Mono<Account> login() {
        Mono<Account> map = Mono.fromFuture(deviceCodeFuture).map(ignore -> this);
        loginMono = super.login().doFinally(r -> executorService.shutdown()).subscribeOn(Schedulers.boundedElastic()).cache();
        return map.doOnCancel(executorService::shutdownNow).doOnSubscribe(ignore -> loginMono.subscribe());
    }

    public Mono<Account> continueLogin() {
        return loginMono;
    }

    public DeviceCodeInfo getDeviceCode() {
        try {
            return this.deviceCodeFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new AzureToolkitAuthenticationException(e.getMessage());
        }
    }

    private TokenCredential createCredential(AzureEnvironment env) {
        if (executorService.isShutdown()) {
            throw new AzureToolkitAuthenticationException("Cannot device login twice.");
        }
        AzureEnvironmentUtils.setupAzureEnvironment(env);
        DeviceCodeCredentialBuilder builder = new DeviceCodeCredentialBuilder();
        if (isEnablePersistence()) {
            builder.tokenCachePersistenceOptions(new TokenCachePersistenceOptions().setName(TOOLKIT_TOKEN_CACHE_NAME));
        }
        return builder.clientId(IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID)
                .executorService(executorService)
                .challengeConsumer(deviceCodeFuture::complete).build();
    }

    private static ExecutorService createExecutorService() {
        return Executors.newFixedThreadPool(2, namedThreadFactory);
    }
}
