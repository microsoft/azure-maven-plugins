/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.task;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Func2;
import rx.plugins.RxJavaHooks;

import java.util.Objects;

public class AzureRxTaskManager {
    private static boolean registered = false;

    @SuppressWarnings("rawtypes")
    public static synchronized void register() {
        if (registered) {
            throw new IllegalStateException("rx task manager has already been registered.");
        }
        registered = true;
        final Func2<Observable, Observable.OnSubscribe, Observable.OnSubscribe> oldObservableStartHooks = RxJavaHooks.getOnObservableStart();
        final Func2<Completable, Completable.OnSubscribe, Completable.OnSubscribe> oldCompletableStartHooks = RxJavaHooks.getOnCompletableStart();
        final Func2<Single, Single.OnSubscribe, Single.OnSubscribe> oldSingleStartHooks = RxJavaHooks.getOnSingleStart();
        RxJavaHooks.setOnObservableStart((observable, onStart) -> {
            final AzureOperationContext context = AzureOperationContext.current().derive();
            final Observable.OnSubscribe<?> withClosure = (subscriber) -> context.run(() -> onStart.call(subscriber));
            if (Objects.isNull(oldObservableStartHooks)) {
                return withClosure;
            }
            return oldObservableStartHooks.call(observable, withClosure);
        });
        RxJavaHooks.setOnCompletableStart((completable, onStart) -> {
            final AzureOperationContext context = AzureOperationContext.current().derive();
            final Completable.OnSubscribe withClosure = (subscriber) -> context.run(() -> onStart.call(subscriber));
            if (Objects.isNull(oldCompletableStartHooks)) {
                return withClosure;
            }
            return oldCompletableStartHooks.call(completable, withClosure);
        });
        RxJavaHooks.setOnSingleStart((single, onStart) -> {
            final AzureOperationContext context = AzureOperationContext.current().derive();
            final Single.OnSubscribe<?> withClosure = (subscriber) -> context.run(() -> onStart.call(subscriber));
            if (Objects.isNull(oldSingleStartHooks)) {
                return withClosure;
            }
            return oldSingleStartHooks.call(single, withClosure);
        });
    }
}
