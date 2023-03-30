/*
 *
 *  * Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import reactor.core.Disposable;

import java.util.concurrent.TimeUnit;

public class StreamingLogTask extends AzureTask<AppServiceAppBase<?, ?, ?>> {
    private final AppServiceAppBase<?, ?, ?> webApp;
    private Disposable subscription;

    public StreamingLogTask(AppServiceAppBase<?, ?, ?> webApp) {
        this.webApp = webApp;
    }

    @Override
    protected AppServiceAppBase<?, ?, ?> doExecute() {
        startStreamingLog();
        return this.webApp;
    }

    private void startStreamingLog() {
        if (!webApp.isStreamingLogSupported()) {
            return;
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Opening streaming log of app({0})...", webApp.getName()));
        messager.debug("###############STREAMING LOG BEGIN##################");
        this.subscription = this.webApp.streamAllLogsAsync()
                .doFinally((type) -> messager.debug("###############STREAMING LOG END##################"))
                .subscribe(messager::debug);
        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (final Exception ignored) {
        } finally {
            stopStreamingLog();
        }
    }

    private synchronized void stopStreamingLog() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
