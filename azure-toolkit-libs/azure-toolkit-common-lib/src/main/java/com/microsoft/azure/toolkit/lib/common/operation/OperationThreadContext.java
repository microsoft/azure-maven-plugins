/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.Optional;

@Log
public class OperationThreadContext {
    private static final ThreadLocal<OperationThreadContext> context = new ThreadLocal<>();

    protected long threadId;
    @Nullable
    protected Operation<?> operation;
    @Getter
    @Nullable
    protected OperationThreadContext parent;

    private OperationThreadContext(@Nullable final OperationThreadContext parent) {
        this.threadId = -1;
        this.setParent(parent);
    }

    private synchronized void setParent(@Nullable OperationThreadContext parent) {
        if (!Objects.equals(this.parent, parent)) {
            this.parent = parent;
            this.operation = Optional.ofNullable(parent).map(p -> p.operation).orElse(null);
        }
    }

    @Nonnull
    public static OperationThreadContext current() {
        OperationThreadContext ctxNode = OperationThreadContext.context.get();
        if (Objects.isNull(ctxNode)) {
            ctxNode = new OperationThreadContext(null);
            OperationThreadContext.context.set(ctxNode);
        }
        return ctxNode;
    }

    @Nullable
    public Operation<?> currentOperation() {
        return this.operation;
    }

    synchronized void pushOperation(final Operation<?> operation) {
        if (Objects.isNull(this.parent) && Objects.isNull(this.operation)) {
            log.fine(String.format("orphan context[%s] is setup", this));
        }
        operation.setParent(this.operation);
        this.operation = operation;
    }

    @Nullable
    synchronized Operation<?> popOperation() {
        final Operation<?> popped = this.operation;
        assert popped != null : "popped operation is null";
        this.operation = popped.getParent();
        if (Objects.isNull(this.parent) && Objects.isNull(this.operation)) {
            OperationThreadContext.context.remove();
            log.fine(String.format("orphan context[%s] is disposed", this));
        }
        return popped;
    }

    public void run(final Runnable runnable) {
        try {
            this.setup();
            runnable.run();
        } catch (final Throwable throwable) {
            final Throwable rootCause = ExceptionUtils.getRootCause(throwable);
            if (!(rootCause instanceof InterruptedIOException) && !(rootCause instanceof InterruptedException)) {
                // Swallow interrupted exception caused by unsubscribe
                AzureMessager.getMessager().error(throwable);
            }
        } finally {
            this.dispose();
        }
    }

    @Nonnull
    public OperationThreadContext derive() {
        final long threadId = Thread.currentThread().getId();
        final OperationThreadContext current = OperationThreadContext.current();
        assert this == current : String.format("[threadId:%s] deriving context from context[%s] in context[%s].", threadId, this, current);
        this.threadId = this.threadId > 0 ? this.threadId : threadId;
        return new OperationThreadContext(this);
    }

    private synchronized void setup() {
        final OperationThreadContext current = OperationThreadContext.current();
        final long threadId = Thread.currentThread().getId();
        assert current.threadId == -1 || current.threadId == threadId : String.format("[threadId:%s] illegal thread context[%s]", threadId, current);
        this.threadId = threadId; // we can not decide in which thread this task will run until here.
        if (current.threadId != -1) {
            this.setParent(current);
        }
        OperationThreadContext.context.set(this);
    }

    private synchronized void dispose() {
        final OperationThreadContext current = OperationThreadContext.current();
        final long threadId = Thread.currentThread().getId();
        assert this == current && this.threadId == threadId : String.format("[threadId:%s] disposing context[%s] in context[%s].", threadId, this, current);
        if (this.parent == null || this.threadId != this.parent.threadId) { // this is the root task of current thread.
            OperationThreadContext.context.remove();
        } else { // this is not the root task of current thread.
            OperationThreadContext.context.set(this.parent);
        }
    }

    public String getId() {
        return Utils.getId(this);
    }

    public String toString() {
        final String id = getId();
        final String prId = Optional.ofNullable(this.parent).map(OperationThreadContext::getId).orElse("/");
        return String.format("{id: %s, threadId:%s, parent:%s}", id, this.threadId, prId);
    }
}
