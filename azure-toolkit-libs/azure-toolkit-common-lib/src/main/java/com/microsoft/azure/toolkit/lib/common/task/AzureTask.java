/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.task;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureText;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
@Setter
public class AzureTask<T> implements IAzureOperation {
    @Nonnull
    private final Modality modality;
    @Getter(AccessLevel.NONE)
    @Nullable
    private final Supplier<T> supplier;
    @Nullable
    private final Object project;
    private final boolean cancellable;
    @Nullable
    private final AzureText title;
    private IAzureOperation parent;
    @Builder.Default
    private boolean backgroundable = true;
    @Nullable
    private Boolean backgrounded = null;

    public AzureTask() {
        this((Supplier<T>) null);
    }

    public AzureTask(@Nonnull Runnable runnable) {
        this(runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull String title, @Nonnull Runnable runnable) {
        this(title, runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull AzureText title, @Nonnull Runnable runnable) {
        this(title, runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Supplier<T> supplier) {
        this(supplier, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull String title, @Nonnull Supplier<T> supplier) {
        this(null, title, false, supplier, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull AzureText title, @Nonnull Supplier<T> supplier) {
        this(null, title, false, supplier, Modality.DEFAULT);
    }

    public AzureTask(@Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(null, (String) null, false, runnable, modality);
    }

    public AzureTask(@Nonnull String title, @Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(null, title, false, runnable, modality);
    }

    public AzureTask(@Nonnull AzureText title, @Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(null, title, false, runnable, modality);
    }

    public AzureTask(@Nullable Supplier<T> supplier, @Nonnull Modality modality) {
        this(null, (String) null, false, supplier, modality);
    }

    public AzureTask(@Nonnull String title, @Nonnull Supplier<T> supplier, @Nonnull Modality modality) {
        this(null, title, false, supplier, modality);
    }

    public AzureTask(@Nonnull AzureText title, @Nonnull Supplier<T> supplier, @Nonnull Modality modality) {
        this(null, title, false, supplier, modality);
    }

    public AzureTask(@Nullable Object project, @Nonnull String title, boolean cancellable, @Nonnull Runnable runnable) {
        this(project, title, cancellable, runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Object project, @Nonnull AzureText title, boolean cancellable, @Nonnull Runnable runnable) {
        this(project, title, cancellable, runnable, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Object project, @Nonnull String title, boolean cancellable, @Nonnull Supplier<T> supplier) {
        this(project, title, cancellable, supplier, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Object project, @Nonnull AzureText title, boolean cancellable, @Nonnull Supplier<T> supplier) {
        this(project, title, cancellable, supplier, Modality.DEFAULT);
    }

    public AzureTask(@Nullable Object project, @Nullable String title, boolean cancellable, @Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(project, Optional.ofNullable(title).map(AzureText::fromText).orElse(null), cancellable, runnable, modality);
    }

    public AzureTask(@Nullable Object project, @Nullable AzureText title, boolean cancellable, @Nonnull Runnable runnable, @Nonnull Modality modality) {
        this(project, title, cancellable, () -> {
            runnable.run();
            return null;
        }, modality);
    }

    public AzureTask(@Nullable Object project, @Nullable String title, boolean cancellable, @Nullable Supplier<T> supplier, @Nonnull Modality modality) {
        this(project, Optional.ofNullable(title).map(AzureText::fromText).orElse(null), cancellable, supplier, modality);
    }

    public AzureTask(@Nullable Object project, @Nullable AzureText title, boolean cancellable, @Nullable Supplier<T> supplier, @Nonnull Modality modality) {
        this.project = project;
        this.title = title;
        this.cancellable = cancellable;
        this.supplier = supplier;
        this.modality = modality;
    }

    @Nonnull
    public String getId() {
        return "&" + Utils.getId(this);
    }

    @Override
    @Nonnull
    public String getName() {
        return Optional.ofNullable(this.getTitle()).map(AzureText::getName).orElse(UNKNOWN_NAME);
    }

    @Override
    @Nonnull
    public String getType() {
        return "ASYNC";
    }

    @Override
    public String toString() {
        return String.format("{name:'%s'}", this.getName());
    }

    @Nonnull
    public Supplier<T> getSupplier() {
        return Optional.ofNullable(this.supplier).orElse(this::execute);
    }

    public T execute() {
        throw new UnsupportedOperationException();
    }

    public enum Modality {
        DEFAULT, ANY, NONE
    }
}
