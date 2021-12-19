/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.task;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.Executable;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Getter
@Setter
public class AzureTask<T> implements IAzureOperation<T> {
    @Nonnull
    private final Modality modality;
    @Getter(AccessLevel.NONE)
    @Nullable
    private final Executable<T> body;
    @Nullable
    private final Object project;
    private final boolean cancellable;
    @Nullable
    private final AzureString title;
    private IAzureOperation<?> parent;
    @Builder.Default
    private boolean backgroundable = true;
    @Nullable
    private Boolean backgrounded = null;
    @Nonnull
    @Builder.Default
    private String type = "ASYNC";
    private Monitor monitor;

    public AzureTask() {
        this((Executable<T>) null);
    }

    public AzureTask(@Nonnull Runnable runnable) {
        this(runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull String title, @Nonnull Runnable runnable) {
        this(title, runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull AzureString title, @Nonnull Runnable runnable) {
        this(title, runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Executable<T> body) {
        this(body, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull String title, @Nonnull Executable<T> body) {
        this(null, title, false, body, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull AzureString title, @Nonnull Executable<T> body) {
        this(null, title, false, body, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(null, (String) null, false, runnable, modality);
    }

    public AzureTask(@Nonnull String title, @Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(null, title, false, runnable, modality);
    }

    public AzureTask(@Nonnull AzureString title, @Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(null, title, false, runnable, modality);
    }

    public AzureTask(@Nullable Executable<T> body, @Nonnull Modality modality) {
        this(null, (String) null, false, body, modality);
    }

    public AzureTask(@Nonnull String title, @Nonnull Executable<T> body, @Nonnull Modality modality) {
        this(null, title, false, body, modality);
    }

    public AzureTask(@Nonnull AzureString title, @Nonnull Executable<T> body, @Nonnull Modality modality) {
        this(null, title, false, body, modality);
    }

    public AzureTask(@Nullable Object project, @Nonnull String title, boolean cancellable, @Nonnull Runnable runnable) {
        this(project, title, cancellable, runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Object project, @Nonnull AzureString title, boolean cancellable, @Nonnull Runnable runnable) {
        this(project, title, cancellable, runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Object project, @Nonnull String title, boolean cancellable, @Nonnull Executable<T> body) {
        this(project, title, cancellable, body, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Object project, @Nonnull AzureString title, boolean cancellable, @Nonnull Executable<T> body) {
        this(project, title, cancellable, body, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Object project, @Nullable String title, boolean cancellable, @Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(project, Optional.ofNullable(title).map(AzureString::fromString).orElse(null), cancellable, runnable, modality);
    }

    public AzureTask(@Nullable Object project, @Nullable AzureString title, boolean cancellable, @Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(project, title, cancellable, () -> {
            runnable.run();
            return null;
        }, modality);
    }

    public AzureTask(@Nullable Object project, @Nullable String title, boolean cancellable, @Nullable Executable<T> body, @Nonnull Modality modality) {
        this(project, Optional.ofNullable(title).map(AzureString::fromString).orElse(null), cancellable, body, modality);
    }

    public AzureTask(@Nullable Object project, @Nullable AzureString title, boolean cancellable, @Nullable Executable<T> body, @Nonnull Modality modality) {
        this.project = project;
        this.title = title;
        this.cancellable = cancellable;
        this.monitor = new DefaultMonitor();
        this.body = body;
        this.modality = modality;
    }

    @Nonnull
    public String getExecutionId() {
        return "&" + IAzureOperation.super.getExecutionId();
    }

    @Override
    public String toString() {
        return String.format("{name:'%s'}", this.getName());
    }

    @Nonnull
    public Executable<T> getBody() {
        return Optional.ofNullable(this.body).orElse(this::doExecute);
    }

    @SneakyThrows
    public final T execute() {
        return this.getBody().execute();
    }

    protected T doExecute() throws Throwable {
        throw new UnsupportedOperationException();
    }

    public enum Modality {
        DEFAULT, ANY, NONE
    }

    public interface Monitor {
        void cancel();

        boolean isCancelled();
    }

    public static class DefaultMonitor implements Monitor {
        private boolean cancelled = false;

        @Override
        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled;
        }
    }
}
