/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.task;

import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import lombok.extern.java.Log;
import rx.Emitter;
import rx.Observable;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Log
public abstract class AzureTaskManager {

    private static AzureTaskManager instance;

    public static synchronized void register(AzureTaskManager manager) {
        if (AzureTaskManager.instance == null) {
            AzureTaskManager.instance = manager;
        }
    }

    public static AzureTaskManager getInstance() {
        return AzureTaskManager.instance;
    }

    public final void read(Runnable task) {
        this.read(new AzureTask<>(task));
    }

    public final void read(String title, Runnable task) {
        this.read(new AzureTask<>(title, task));
    }

    public final void read(IAzureOperationTitle title, Runnable task) {
        this.read(new AzureTask<>(title, task));
    }

    public final void read(AzureTask<Void> task) {
        this.runInObservable(this::doRead, task).subscribe();
    }

    public final void write(Runnable task) {
        this.write(new AzureTask<>(task));
    }

    public final void write(String title, Runnable task) {
        this.write(new AzureTask<>(title, task));
    }

    public final void write(IAzureOperationTitle title, Runnable task) {
        this.write(new AzureTask<>(title, task));
    }

    public final void write(AzureTask<Void> task) {
        this.runInObservable(this::doWrite, task).subscribe();
    }

    public final void runLater(Runnable task) {
        this.runLater(new AzureTask<>(task));
    }

    public final void runLater(String title, Runnable task) {
        this.runLater(new AzureTask<>(title, task));
    }

    public final void runLater(IAzureOperationTitle title, Runnable task) {
        this.runLater(new AzureTask<>(title, task));
    }

    public final void runLater(Runnable task, AzureTask.Modality modality) {
        this.runLater(new AzureTask<>(task, modality));
    }

    public final void runLater(String title, Runnable task, AzureTask.Modality modality) {
        this.runLater(new AzureTask<>(title, task, modality));
    }

    public final void runLater(IAzureOperationTitle title, Runnable task, AzureTask.Modality modality) {
        this.runLater(new AzureTask<>(title, task, modality));
    }

    public final void runLater(AzureTask<Void> task) {
        this.runInObservable(this::doRunLater, task).subscribe();
    }

    public final void runAndWait(Runnable task) {
        this.runAndWait(new AzureTask<>(task));
    }

    public final void runAndWait(String title, Runnable task) {
        this.runAndWait(new AzureTask<>(title, task));
    }

    public final void runAndWait(IAzureOperationTitle title, Runnable task) {
        this.runAndWait(new AzureTask<>(title, task));
    }

    public final void runAndWait(Runnable task, AzureTask.Modality modality) {
        this.runAndWait(new AzureTask<>(task, modality));
    }

    public final void runAndWait(String title, Runnable task, AzureTask.Modality modality) {
        this.runAndWait(new AzureTask<>(title, task, modality));
    }

    public final void runAndWait(IAzureOperationTitle title, Runnable task, AzureTask.Modality modality) {
        this.runAndWait(new AzureTask<>(title, task, modality));
    }

    public final void runAndWait(AzureTask<Void> task) {
        this.runInObservable(this::doRunAndWait, task).subscribe();
    }

    public final void runInBackground(String title, Runnable task) {
        this.runInBackground(new AzureTask<>(title, task));
    }

    public final void runInBackground(IAzureOperationTitle title, Runnable task) {
        this.runInBackground(new AzureTask<>(title, task));
    }

    public final void runInBackground(String title, Supplier<Void> task) {
        this.runInBackground(new AzureTask<>(title, task));
    }

    public final void runInBackground(IAzureOperationTitle title, Supplier<Void> task) {
        this.runInBackground(new AzureTask<>(title, task));
    }

    public final void runInBackground(String title, boolean cancellable, Runnable task) {
        this.runInBackground(new AzureTask<>(null, title, cancellable, task));
    }

    public final void runInBackground(IAzureOperationTitle title, boolean cancellable, Runnable task) {
        this.runInBackground(new AzureTask<>(null, title, cancellable, task));
    }

    public final void runInBackground(String title, boolean cancellable, Supplier<Void> task) {
        this.runInBackground(new AzureTask<>(null, title, cancellable, task));
    }

    public final void runInBackground(IAzureOperationTitle title, boolean cancellable, Supplier<Void> task) {
        this.runInBackground(new AzureTask<>(null, title, cancellable, task));
    }

    public final void runInBackground(AzureTask<Void> task) {
        this.runInObservable(this::doRunInBackground, task).subscribe();
    }

    public final void runInModal(String title, Runnable task) {
        this.runInModal(new AzureTask<>(title, task));
    }

    public final void runInModal(IAzureOperationTitle title, Runnable task) {
        this.runInModal(new AzureTask<>(title, task));
    }

    public final void runInModal(String title, Supplier<Void> task) {
        this.runInModal(new AzureTask<>(title, task));
    }

    public final void runInModal(IAzureOperationTitle title, Supplier<Void> task) {
        this.runInModal(new AzureTask<>(title, task));
    }

    public final void runInModal(String title, boolean cancellable, Runnable task) {
        this.runInModal(new AzureTask<>(null, title, cancellable, task));
    }

    public final void runInModal(IAzureOperationTitle title, boolean cancellable, Runnable task) {
        this.runInModal(new AzureTask<>(null, title, cancellable, task));
    }

    public final void runInModal(String title, boolean cancellable, Supplier<Void> task) {
        this.runInModal(new AzureTask<>(null, title, cancellable, task));
    }

    public final void runInModal(IAzureOperationTitle title, boolean cancellable, Supplier<Void> task) {
        this.runInModal(new AzureTask<>(null, title, cancellable, task));
    }

    public final void runInModal(AzureTask<Void> task) {
        this.runInObservable(this::doRunInModal, task).subscribe();
    }

    public final Observable<Void> readAsObservable(Runnable task) {
        return this.readAsObservable(new AzureTask<>(task));
    }

    public final Observable<Void> readAsObservable(String title, Runnable task) {
        return this.readAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> readAsObservable(IAzureOperationTitle title, Runnable task) {
        return this.readAsObservable(new AzureTask<>(title, task));
    }

    public final <T> Observable<T> readAsObservable(AzureTask<T> task) {
        return this.runInObservable(this::doRead, task);
    }

    public final Observable<Void> writeAsObservable(Runnable task) {
        return this.writeAsObservable(new AzureTask<>(task));
    }

    public final Observable<Void> writeAsObservable(String title, Runnable task) {
        return this.writeAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> writeAsObservable(IAzureOperationTitle title, Runnable task) {
        return this.writeAsObservable(new AzureTask<>(title, task));
    }

    public final <T> Observable<T> writeAsObservable(AzureTask<T> task) {
        return this.runInObservable(this::doWrite, task);
    }

    public final Observable<Void> runLaterAsObservable(Runnable task) {
        return this.runLaterAsObservable(new AzureTask<>(task));
    }

    public final Observable<Void> runLaterAsObservable(String title, Runnable task) {
        return this.runLaterAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> runLaterAsObservable(IAzureOperationTitle title, Runnable task) {
        return this.runLaterAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> runLaterAsObservable(Runnable task, AzureTask.Modality modality) {
        return this.runLaterAsObservable(new AzureTask<>(task, modality));
    }

    public final Observable<Void> runLaterAsObservable(String title, Runnable task, AzureTask.Modality modality) {
        return this.runLaterAsObservable(new AzureTask<>(title, task, modality));
    }

    public final Observable<Void> runLaterAsObservable(IAzureOperationTitle title, Runnable task, AzureTask.Modality modality) {
        return this.runLaterAsObservable(new AzureTask<>(title, task, modality));
    }

    public final <T> Observable<T> runLaterAsObservable(AzureTask<T> task) {
        return this.runInObservable(this::doRunLater, task);
    }

    public final Observable<Void> runAndWaitAsObservable(Runnable task) {
        return this.runAndWaitAsObservable(new AzureTask<>(task));
    }

    public final Observable<Void> runAndWaitAsObservable(String title, Runnable task) {
        return this.runAndWaitAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> runAndWaitAsObservable(IAzureOperationTitle title, Runnable task) {
        return this.runAndWaitAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> runAndWaitAsObservable(Runnable task, AzureTask.Modality modality) {
        return this.runAndWaitAsObservable(new AzureTask<>(task, modality));
    }

    public final Observable<Void> runAndWaitAsObservable(String title, Runnable task, AzureTask.Modality modality) {
        return this.runAndWaitAsObservable(new AzureTask<>(title, task, modality));
    }

    public final Observable<Void> runAndWaitAsObservable(IAzureOperationTitle title, Runnable task, AzureTask.Modality modality) {
        return this.runAndWaitAsObservable(new AzureTask<>(title, task, modality));
    }

    public final <T> Observable<T> runAndWaitAsObservable(AzureTask<T> task) {
        return this.runInObservable(this::doRunAndWait, task);
    }

    public final Observable<Void> runInBackgroundAsObservable(String title, Runnable task) {
        return this.runInBackgroundAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> runInBackgroundAsObservable(IAzureOperationTitle title, Runnable task) {
        return this.runInBackgroundAsObservable(new AzureTask<>(title, task));
    }

    public final <T> Observable<T> runInBackgroundAsObservable(String title, Supplier<T> task) {
        return this.runInBackgroundAsObservable(new AzureTask<>(title, task));
    }

    public final <T> Observable<T> runInBackgroundAsObservable(IAzureOperationTitle title, Supplier<T> task) {
        return this.runInBackgroundAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> runInBackgroundAsObservable(String title, boolean cancellable, Runnable task) {
        return this.runInBackgroundAsObservable(new AzureTask<>(null, title, cancellable, task));
    }

    public final Observable<Void> runInBackgroundAsObservable(IAzureOperationTitle title, boolean cancellable, Runnable task) {
        return this.runInBackgroundAsObservable(new AzureTask<>(null, title, cancellable, task));
    }

    public final <T> Observable<T> runInBackgroundAsObservable(String title, boolean cancellable, Supplier<T> task) {
        return this.runInBackgroundAsObservable(new AzureTask<>(null, title, cancellable, task));
    }

    public final <T> Observable<T> runInBackgroundAsObservable(IAzureOperationTitle title, boolean cancellable, Supplier<T> task) {
        return this.runInBackgroundAsObservable(new AzureTask<>(null, title, cancellable, task));
    }

    public final <T> Observable<T> runInBackgroundAsObservable(AzureTask<T> task) {
        return this.runInObservable(this::doRunInBackground, task);
    }

    public final Observable<Void> runInModalAsObservable(String title, Runnable task) {
        return this.runInModalAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> runInModalAsObservable(IAzureOperationTitle title, Runnable task) {
        return this.runInModalAsObservable(new AzureTask<>(title, task));
    }

    public final <T> Observable<T> runInModalAsObservable(String title, Supplier<T> task) {
        return this.runInModalAsObservable(new AzureTask<>(title, task));
    }

    public final <T> Observable<T> runInModalAsObservable(IAzureOperationTitle title, Supplier<T> task) {
        return this.runInModalAsObservable(new AzureTask<>(title, task));
    }

    public final Observable<Void> runInModalAsObservable(String title, boolean cancellable, Runnable task) {
        return this.runInModalAsObservable(new AzureTask<>(null, title, cancellable, task));
    }

    public final Observable<Void> runInModalAsObservable(IAzureOperationTitle title, boolean cancellable, Runnable task) {
        return this.runInModalAsObservable(new AzureTask<>(null, title, cancellable, task));
    }

    public final <T> Observable<T> runInModalAsObservable(String title, boolean cancellable, Supplier<T> task) {
        return this.runInModalAsObservable(new AzureTask<>(null, title, cancellable, task));
    }

    public final <T> Observable<T> runInModalAsObservable(IAzureOperationTitle title, boolean cancellable, Supplier<T> task) {
        return this.runInModalAsObservable(new AzureTask<>(null, title, cancellable, task));
    }

    public final <T> Observable<T> runInModalAsObservable(AzureTask<T> task) {
        return this.runInObservable(this::doRunInModal, task);
    }

    private <T> Observable<T> runInObservable(final BiConsumer<? super Runnable, ? super AzureTask<T>> consumer, final AzureTask<T> task) {
        return Observable.create((Emitter<T> emitter) -> {
            final AzureTaskContext.Node context = AzureTaskContext.current().derive();
            task.setContext(context);
            context.setTask(task);
            AzureTelemeter.afterCreate(task);
            final Runnable t = () -> AzureTaskContext.run(() -> {
                try {
                    emitter.onNext(task.getSupplier().get());
                    emitter.onCompleted();
                } catch (final Throwable e) {
                    emitter.onError(e);
                }
            }, context);
            consumer.accept(t, task);
        }, Emitter.BackpressureMode.BUFFER);
    }

    protected abstract void doRead(Runnable runnable, AzureTask<?> task);

    protected abstract void doWrite(Runnable runnable, AzureTask<?> task);

    protected abstract void doRunLater(Runnable runnable, AzureTask<?> task);

    protected abstract void doRunAndWait(Runnable runnable, AzureTask<?> task);

    protected abstract void doRunInBackground(Runnable runnable, AzureTask<?> task);

    protected abstract void doRunInModal(Runnable runnable, AzureTask<?> task);
}
