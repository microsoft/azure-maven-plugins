/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.FixedDelay;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.appcontainers.models.ContainerApp;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static final String TENANTID = "72f988bf-86f1-41af-91ab-2d7cd011db47";
    public static final String SUBID = "685ba005-af8d-4b04-8f16-a7bf38b2eb5a";

    public static void main(String[] args) {
        AzureTaskManager.register(getManager());
        AzureMessager.setDefaultMessager(getMessager());
        final Account account = Azure.az(AzureAccount.class).login(AuthType.AZURE_CLI);
        account.setSelectedSubscriptions(Collections.singletonList(SUBID));
        AzureProfile profile = new AzureProfile(TENANTID, SUBID, AzureEnvironment.AZURE);
        TokenCredential credential = account.getTokenCredential(SUBID);
        ContainerAppsApiManager manager = ContainerAppsApiManager.configure()
            .withPolicy((httpPipelineCallContext, httpPipelineNextPolicy) -> {
                final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
                httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("wangmi/0.1 %s", previousUserAgent));
                return httpPipelineNextPolicy.process();
            })
            .withRetryPolicy(new RetryPolicy(new FixedDelay(0, Duration.ofSeconds(0))))
            .authenticate(credential, profile);
        final List<ContainerApp> apps = manager.containerApps().list().stream().collect(Collectors.toList());
        final ContainerApp app = apps.stream().filter(a -> a.name().equals("wangmi-test2")).findFirst().get();
        final ContainerAppDraft draft = new ContainerAppDraft();
        final ContainerAppDraft.Config config = new ContainerAppDraft.Config();
        final List<ContainerRegistry> registries = Azure.az(AzureContainerRegistry.class).registry(SUBID).list();
        config.setContainerRegistry(registries.get(1));
        config.setImageId("wangmitest.azurecr.io/containerapps-albumapi-javascript:v1");
        draft.setConfig(config);
        draft.deployImage(app);
    }

    @Nonnull
    private static AzureMessager getMessager() {
        return new AzureMessager() {
            @Override
            public boolean show(IAzureMessage message) {
                System.out.println(message.getContent());
                return true;
            }
        };
    }

    @Nonnull
    private static AzureTaskManager getManager() {
        return new AzureTaskManager() {
            @Override
            protected void doRead(Runnable runnable, AzureTask<?> task) {
                runnable.run();
            }

            @Override
            protected void doWrite(Runnable runnable, AzureTask<?> task) {
                runnable.run();
            }

            @Override
            protected void doRunLater(Runnable runnable, AzureTask<?> task) {
                runnable.run();
            }

            @Override
            protected void doRunOnPooledThread(Runnable runnable, AzureTask<?> task) {
                runnable.run();
            }

            @Override
            protected void doRunAndWait(Runnable runnable, AzureTask<?> task) {
                runnable.run();
            }

            @Override
            protected void doRunInBackground(Runnable runnable, AzureTask<?> task) {
                runnable.run();
            }

            @Override
            protected void doRunInModal(Runnable runnable, AzureTask<?> task) {
                runnable.run();
            }
        };
    }
}
