/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.task;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@Log
public class AzureTaskContext {
    private static final ThreadLocal<AzureTaskContext> context = new ThreadLocal<>();

    protected long threadId;
    @Nullable
    protected IAzureOperation operation;
    @Getter
    @Nullable
    protected AzureTaskContext parent;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    @Nullable
    private AzureTask<?> task;

    private AzureTaskContext(@Nullable final AzureTaskContext parent) {
        this.operation = Optional.ofNullable(parent).map(p -> p.operation).orElse(null);
        this.threadId = -1;
        this.parent = parent;
    }

    @Nonnull
    public static AzureTaskContext current() {
        AzureTaskContext ctxNode = AzureTaskContext.context.get();
        if (Objects.isNull(ctxNode)) {
            ctxNode = new AzureTaskContext(null);
            AzureTaskContext.context.set(ctxNode);
        }
        return ctxNode;
    }

    @Nullable
    public IAzureOperation currentOperation() {
        return this.operation;
    }

    public void pushOperation(final IAzureOperation operation) {
        if (Objects.isNull(this.parent) && Objects.isNull(this.operation)) {
            log.fine(String.format("orphan context[%s] is setup", this));
        }
        operation.setParent(this.operation);
        this.operation = operation;
    }

    @Nullable
    public IAzureOperation popOperation() {
        final IAzureOperation popped = this.operation;
        assert popped != null : "popped operation is null";
        this.operation = popped.getParent();
        if (Objects.isNull(this.parent) && Objects.isNull(this.operation)) {
            AzureTaskContext.context.remove();
            log.fine(String.format("orphan context[%s] is disposed", this));
        }
        return popped;
    }

    public static void run(final Runnable runnable, AzureTaskContext context) {
        try {
            context.setup();
            Optional.ofNullable(context.getTask()).ifPresent(task -> {
                AzureTelemeter.beforeEnter(task);
                AzureTaskContext.current().pushOperation(task);
            });
            runnable.run();
            Optional.ofNullable(context.getTask()).ifPresent(task -> {
                final IAzureOperation popped = AzureTaskContext.current().popOperation();
                AzureTelemeter.afterExit(task);
                assert Objects.equals(task, popped) : String.format("popped op[%s] is not the exiting async task[%s]", popped, task);
            });
        } catch (final Throwable throwable) {
            AzureExceptionHandler.onRxException(throwable);
            Optional.ofNullable(context.getTask()).ifPresent(task -> {
                final IAzureOperation popped = AzureTaskContext.current().popOperation();
                AzureTelemeter.onError(task, throwable);
                assert Objects.equals(task, popped) : String.format("popped op[%s] is not the task[%s] throwing exception", popped, task);
            });
        } finally {
            context.dispose();
        }
    }

    @Nonnull
    AzureTaskContext derive() {
        final long threadId = Thread.currentThread().getId();
        final AzureTaskContext current = AzureTaskContext.current();
        assert this == current : String.format("[threadId:%s] deriving context from context[%s] in context[%s].", threadId, this, current);
        this.threadId = this.threadId > 0 ? this.threadId : threadId;
        return new AzureTaskContext(this);
    }

    private void setup() {
        final AzureTaskContext current = AzureTaskContext.current();
        final long threadId = Thread.currentThread().getId();
        assert current.threadId == -1 || current.threadId == threadId : String.format("[threadId:%s] illegal thread context[%s]", threadId, current);
        this.threadId = threadId; // we can not decide in which thread this task will run until here.
        this.parent = current;
        AzureTaskContext.context.set(this);
    }

    private void dispose() {
        final AzureTaskContext current = AzureTaskContext.current();
        final long threadId = Thread.currentThread().getId();
        assert this == current && this.threadId == threadId : String.format("[threadId:%s] disposing context[%s] in context[%s].", threadId, this, current);
        if (this.parent == null || this.threadId != this.parent.threadId) { // this is the root task of current thread.
            AzureTaskContext.context.remove();
        } else { // this is not the root task of current thread.
            AzureTaskContext.context.set(this.parent);
        }
    }

    public String getId() {
        return Utils.getId(this);
    }

    public String toString() {
        final String id = getId();
        final String prId = Optional.ofNullable(this.parent).map(AzureTaskContext::getId).orElse("/");
        return String.format("{id: %s, threadId:%s, parent:%s}", id, this.threadId, prId);
    }
}
