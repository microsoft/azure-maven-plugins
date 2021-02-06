/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.task;

import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.function.Supplier;

@Getter
public class AzureTask<T> implements IAzureOperation {
    private final Modality modality;
    private final Supplier<T> supplier;
    private final Object project;
    private final boolean cancellable;
    @Builder.Default
    @Setter
    private boolean backgroundable = true;
    private final IAzureOperationTitle title;

    @Setter(AccessLevel.PACKAGE)
    private AzureTaskContext.Node context;

    public AzureTask(Runnable runnable) {
        this(runnable, Modality.DEFAULT);
    }

    public AzureTask(String title, Runnable runnable) {
        this(title, runnable, Modality.DEFAULT);
    }

    public AzureTask(IAzureOperationTitle title, Runnable runnable) {
        this(title, runnable, Modality.DEFAULT);
    }

    public AzureTask(Supplier<T> supplier) {
        this(supplier, Modality.DEFAULT);
    }

    public AzureTask(String title, Supplier<T> supplier) {
        this(null, title, false, supplier, Modality.DEFAULT);
    }

    public AzureTask(IAzureOperationTitle title, Supplier<T> supplier) {
        this(null, title, false, supplier, Modality.DEFAULT);
    }

    public AzureTask(Runnable runnable, Modality modality) {
        this(null, (String) null, false, runnable, modality);
    }

    public AzureTask(String title, Runnable runnable, Modality modality) {
        this(null, title, false, runnable, modality);
    }

    public AzureTask(IAzureOperationTitle title, Runnable runnable, Modality modality) {
        this(null, title, false, runnable, modality);
    }

    public AzureTask(Supplier<T> supplier, Modality modality) {
        this(null, (String) null, false, supplier, modality);
    }

    public AzureTask(String title, Supplier<T> supplier, Modality modality) {
        this(null, title, false, supplier, modality);
    }

    public AzureTask(IAzureOperationTitle title, Supplier<T> supplier, Modality modality) {
        this(null, title, false, supplier, modality);
    }

    public AzureTask(Object project, String title, boolean cancellable, Runnable runnable) {
        this(project, title, cancellable, runnable, Modality.DEFAULT);
    }

    public AzureTask(Object project, IAzureOperationTitle title, boolean cancellable, Runnable runnable) {
        this(project, title, cancellable, runnable, Modality.DEFAULT);
    }

    public AzureTask(Object project, String title, boolean cancellable, Supplier<T> supplier) {
        this(project, title, cancellable, supplier, Modality.DEFAULT);
    }

    public AzureTask(Object project, IAzureOperationTitle title, boolean cancellable, Supplier<T> supplier) {
        this(project, title, cancellable, supplier, Modality.DEFAULT);
    }

    public AzureTask(Object project, String title, boolean cancellable, Runnable runnable, Modality modality) {
        this(project, new IAzureOperationTitle.Simple(title), cancellable, runnable, modality);
    }

    public AzureTask(Object project, IAzureOperationTitle title, boolean cancellable, Runnable runnable, Modality modality) {
        this(project, title, cancellable, () -> {
            runnable.run();
            return null;
        }, modality);
    }

    public AzureTask(Object project, String title, boolean cancellable, Supplier<T> supplier, Modality modality) {
        this(project, new IAzureOperationTitle.Simple(title), cancellable, supplier, modality);
    }

    public AzureTask(Object project, IAzureOperationTitle title, boolean cancellable, Supplier<T> supplier, Modality modality) {
        this.project = project;
        this.title = title;
        this.cancellable = cancellable;
        this.supplier = supplier;
        this.modality = modality;
    }

    public String getId() {
        return "&" + Utils.getId(this);
    }

    @Override
    public String getName() {
        return Optional.ofNullable(this.getTitle()).map(IAzureOperationTitle::getName).orElse("<unknown>.<unknown>");
    }

    @Override
    public String getType() {
        return "ASYNC";
    }

    public enum Modality {
        DEFAULT, ANY, NONE
    }
}
