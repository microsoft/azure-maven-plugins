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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

@Log
public abstract class AzureTaskContext {
    private static final ThreadLocal<Node> context = new ThreadLocal<>();

    protected long threadId = -1;
    protected final Deque<IAzureOperation> operations;

    private AzureTaskContext() {
        this.operations = new ArrayDeque<>();
    }

    private AzureTaskContext(final long threadId, final Deque<IAzureOperation> operations) {
        this.operations = operations;
        this.threadId = threadId;
    }

    public Deque<IAzureOperation> getOperations() {
        return new ArrayDeque<>(this.operations);
    }

    public static Deque<IAzureOperation> getContextOperations(Node node) {
        final Deque<IAzureOperation> ops = new ArrayDeque<>(node.operations);
        AzureTaskContext parent = node.parent;
        while (Objects.nonNull(parent)) {
            ops.addAll(parent.operations);
            if (parent instanceof Node) {
                parent = ((Node) parent).parent;
            } else {
                break;
            }
        }
        return ops;
    }

    public static Deque<IAzureOperation> getContextOperations() {
        return getContextOperations(AzureTaskContext.current());
    }

    public static Node current() {
        Node ctxNode = AzureTaskContext.context.get();
        if (Objects.isNull(ctxNode)) {
            ctxNode = new Node(null);
            AzureTaskContext.context.set(ctxNode);
        }
        return ctxNode;
    }

    public static void run(final Runnable runnable, Node context) {
        try {
            context.setup();
            Optional.ofNullable(context.getTask()).ifPresent(task -> {
                AzureTelemeter.beforeEnter(task);
                AzureTaskContext.current().pushOperation(task);
            });
            runnable.run();
        } catch (final Throwable throwable) {
            AzureExceptionHandler.onRxException(throwable);
            Optional.ofNullable(context.getTask()).ifPresent(task -> {
                AzureTelemeter.onError(task, throwable);
            });
        } finally {
            Optional.ofNullable(context.getTask()).ifPresent(task -> {
                final IAzureOperation popped = AzureTaskContext.current().popOperation();
                AzureTelemeter.afterExit(task);
                assert Objects.equals(task, popped) : String.format("popped op[%s] is not the exiting async task[%s]", popped, task);
            });
            context.dispose();
        }
    }

    public String getId() {
        return Utils.getId(this);
    }

    public static class Node extends AzureTaskContext {
        @Setter
        @Getter
        private Boolean backgrounded = null;
        @Getter
        private AzureTaskContext parent;
        protected boolean disposed;
        @Getter
        @Setter(AccessLevel.PACKAGE)
        private AzureTask<?> task;
        private boolean async = false;

        private Node(final AzureTaskContext parent) {
            super();
            this.parent = parent;
        }

        public boolean isOrphan() {
            return Objects.isNull(this.parent);
        }

        public void pushOperation(final IAzureOperation operation) {
            if (this.isOrphan()) {
                log.info(String.format("orphan context[%s] is setup", this));
            }
            this.operations.push(operation);
        }

        @Nullable
        public IAzureOperation popOperation() {
            final IAzureOperation popped = this.operations.pop();
            if (this.isOrphan() && this.operations.isEmpty()) {
                AzureTaskContext.context.remove();
                log.info(String.format("orphan context[%s] is disposed", this));
            }
            return popped;
        }

        Node derive() {
            final long threadId = Thread.currentThread().getId();
            final Node current = AzureTaskContext.current();
            assert this == current : String.format("[threadId:%s] deriving context from context[%s] in context[%s].", threadId, this, current);
            if (this.disposed) {
                log.warning(String.format("[threadId:%s] deriving from a disposed context[%s]", threadId, this));
            }
            this.threadId = this.threadId > 0 ? this.threadId : threadId;
            final Snapshot snapshot = new Snapshot(this);
            return new Node(snapshot);
        }

        private void setup() {
            final Node current = AzureTaskContext.current();
            final long threadId = Thread.currentThread().getId();
            assert current.threadId == -1 || current.threadId == threadId : String.format("[threadId:%s] illegal thread context[%s]", threadId, current);
            if (this.threadId > 0 || this.disposed) {
                log.warning(String.format("[threadId:%s] context[%s] already setup/disposed", threadId, this));
            }
            this.threadId = threadId; // we can not decide in which thread this task will run until here.
            this.async = threadId != current.threadId;
            if (threadId == current.threadId) { // this task runs in the same thread as parent.
                this.parent = current;
            }
            AzureTaskContext.context.set(this);
        }

        private void dispose() {
            final Node current = AzureTaskContext.current();
            final long threadId = Thread.currentThread().getId();
            assert this == current && this.threadId == threadId : String.format("[threadId:%s] disposing context[%s] in context[%s].", threadId, this, current);
            if (this.disposed) {
                log.warning(String.format("[threadId:%s] disposing a disposed context[%s].", threadId, this));
            }
            this.disposed = true;
            if (this.parent instanceof Node) { // this is not the root task of current thread.
                assert this.threadId == this.parent.threadId : String.format("current[%s].threadId != current.parent[%s].threadId", this, this.parent);
                assert !this.async : String.format("disposing async task context[%s](parent[%s]) in sync mode", this, this.parent);
                AzureTaskContext.context.set((Node) this.parent);
            } else { // this is the root task of current thread.
                assert this.async : String.format("disposing sync task context[%s](parent[%s]) in async mode", this, this.parent);
                if (this.threadId == this.parent.threadId) {
                    log.warning(String.format("[threadId:%s] thread/threadId is reused.", threadId));
                }
                AzureTaskContext.context.remove();
            }
        }

        public String toString() {
            final String id = getId();
            if (this.parent instanceof Snapshot) {
                final String orId = ((Snapshot) this.parent).origin.getId();
                return String.format("{id: %s, threadId:%s, snapshot@parent.origin:%s, disposed:%s}", id, this.threadId, orId, this.disposed);
            } else {
                final String prId = Optional.ofNullable(this.parent).map(AzureTaskContext::getId).orElse("/");
                return String.format("{id: %s, threadId:%s, parent:%s, disposed:%s}", id, this.threadId, prId, this.disposed);
            }
        }
    }

    public static class Snapshot extends AzureTaskContext {
        @Getter
        private final Node origin; // snapshot refers original context

        private Snapshot(@Nonnull final AzureTaskContext.Node origin) {
            super(origin.threadId, AzureTaskContext.getContextOperations(origin));
            this.origin = origin;
        }

        public String toString() {
            final String id = getId();
            final String orId = this.origin.getId();
            return String.format("{id: %s, threadId:%s, origin:%s}", id, this.threadId, orId);
        }
    }
}
