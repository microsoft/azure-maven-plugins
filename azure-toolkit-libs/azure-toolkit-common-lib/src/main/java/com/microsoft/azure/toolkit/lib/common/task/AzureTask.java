/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.task;

import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class AzureTask<T> {
    private final Modality modality;
    private final Supplier<T> supplier;
    private final Object project;
    private final boolean cancellable;
    private final String title;

    public AzureTask(Runnable runnable) {
        this(runnable, Modality.DEFAULT);
    }

    public AzureTask(String title, Runnable runnable) {
        this(title, runnable, Modality.DEFAULT);
    }

    public AzureTask(Supplier<T> supplier) {
        this(supplier, Modality.DEFAULT);
    }

    public AzureTask(String title, Supplier<T> supplier) {
        this(null, title, false, supplier, Modality.DEFAULT);
    }

    public AzureTask(Runnable runnable, Modality modality) {
        this(null, null, false, runnable, modality);
    }

    public AzureTask(String title, Runnable runnable, Modality modality) {
        this(null, title, false, runnable, modality);
    }

    public AzureTask(Supplier<T> supplier, Modality modality) {
        this(null, null, false, supplier, modality);
    }

    public AzureTask(String title, Supplier<T> supplier, Modality modality) {
        this(null, title, false, supplier, modality);
    }

    public AzureTask(Object project, String title, boolean cancellable, Runnable runnable) {
        this(project, title, cancellable, runnable, Modality.DEFAULT);
    }

    public AzureTask(Object project, String title, boolean cancellable, Supplier<T> supplier) {
        this(project, title, cancellable, supplier, Modality.DEFAULT);
    }

    public AzureTask(Object project, String title, boolean cancellable, Runnable runnable, Modality modality) {
        this(project, title, cancellable, () -> {
            runnable.run();
            return null;
        }, modality);
    }

    public AzureTask(Object project, String title, boolean cancellable, Supplier<T> supplier, Modality modality) {
        this.project = project;
        this.title = title;
        this.cancellable = cancellable;
        this.supplier = supplier;
        this.modality = modality;
    }

    public T go() {
        return this.supplier.get();
    }

    public String getTitle() {
        return this.title;
    }

    public enum Modality {
        DEFAULT, ANY, NONE
    }

}
