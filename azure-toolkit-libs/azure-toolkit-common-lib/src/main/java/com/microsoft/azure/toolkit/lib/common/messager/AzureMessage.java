/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.management.exception.ManagementException;
import com.google.common.collect.Streams;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureText;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Setter
@RequiredArgsConstructor
public abstract class AzureMessage implements IAzureMessage {
    static final String DEFAULT_MESSAGE_TITLE = "Azure";
    @Nullable
    private String title;
    @Nullable
    private Object payload;
    @Nullable
    private Action[] actions;
    @Nullable
    private Boolean backgrounded;
    @Nonnull
    @Getter
    private final IAzureMessage original;
    private ArrayList<IAzureOperation> operations;

    public AzureMessage(@Nonnull Type type, @Nonnull String message) {
        this(new SimpleMessage(type, message));
    }

    @Nonnull
    public String getMessage() {
        if (!(getPayload() instanceof Throwable)) {
            return this.original.getMessage();
        }
        final Throwable throwable = (Throwable) getPayload();
        final List<IAzureOperation> operations = this.getOperations();
        final String failure = operations.isEmpty() ? "Failed to proceed" : "Failed to " + getText(operations.get(0).getTitle());
        final String cause = Optional.ofNullable(this.getCause(throwable))
                .map(c -> String.format(", because %s", c))
                .orElse("");
        final String errorAction = Optional.ofNullable(this.getErrorAction(throwable)).orElse("");
        return failure + cause + "\n" + errorAction;
    }

    public String getDetails() {
        final List<IAzureOperation> operations = this.getOperations();
        return operations.size() < 2 ? "" : operations.stream()
                .map(this::getDetailItem)
                .collect(Collectors.joining("", "\n", ""));
    }

    protected String getDetailItem(IAzureOperation o) {
        return String.format("● %s", StringUtils.capitalize(getText(o.getTitle())));
    }

    public String getText(@Nullable AzureText text) {
        return Optional.ofNullable(text).map(AzureText::getText).orElse(null);
    }

    @Nullable
    protected String getCause(@Nonnull Throwable throwable) {
        final Throwable root = getRecognizableCause(throwable);
        if (Objects.isNull(root)) {
            return ExceptionUtils.getRootCause(throwable).toString();
        }
        String cause = null;
        if (root instanceof ManagementException) {
            cause = ((ManagementException) root).getValue().getMessage();
        } else if (root instanceof HttpResponseException) {
            cause = ((HttpResponseException) root).getResponse().getBodyAsString().block();
        }
        return StringUtils.firstNonBlank(cause, root.getMessage());
    }

    @Nullable
    private static Throwable getRecognizableCause(@Nonnull Throwable throwable) {
        final List<Throwable> throwables = ExceptionUtils.getThrowableList(throwable);
        for (int i = throwables.size() - 1; i >= 0; i--) {
            final Throwable t = throwables.get(i);
            if (t instanceof AzureOperationException) {
                continue;
            }
            final String rootClassName = t.getClass().getName();
            if (rootClassName.startsWith("com.microsoft") || rootClassName.startsWith("com.azure")) {
                return t;
            }
        }
        return null;
    }

    @Nullable
    protected String getErrorAction(@Nonnull Throwable throwable) {
        return ExceptionUtils.getThrowableList(throwable).stream()
                .filter(t -> t instanceof AzureToolkitRuntimeException || t instanceof AzureToolkitException)
                .map(t -> t instanceof AzureToolkitRuntimeException ? ((AzureToolkitRuntimeException) t).getAction() : ((AzureToolkitException) t).getAction())
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    @Nonnull
    protected List<IAzureOperation> getOperations() {
        if (Objects.isNull(this.operations)) {
            final List<IAzureOperation> contextOperations = getContextOperations();
            final List<IAzureOperation> exceptionOperations = Optional.ofNullable(this.getPayload())
                    .filter(p -> p instanceof Throwable)
                    .map(p -> getExceptionOperations((Throwable) p))
                    .orElse(new ArrayList<>());
            final Map<String, IAzureOperation> operations = new HashMap<>();
            Streams.concat(contextOperations.stream(), exceptionOperations.stream())
                    .filter(o -> !operations.containsKey(o.getName()))
                    .forEachOrdered(o -> operations.put(o.getName(), o));
            this.operations = new ArrayList<>(operations.values());
        }
        return this.operations;
    }

    @Nonnull
    private static List<IAzureOperation> getContextOperations() {
        final LinkedList<IAzureOperation> result = new LinkedList<>();
        IAzureOperation current = AzureTaskContext.current().currentOperation();
        while (Objects.nonNull(current)) {
            if (current instanceof AzureOperationRef) {
                result.addFirst(current);
                final AzureOperation annotation = ((AzureOperationRef) current).getAnnotation(AzureOperation.class);
                if (annotation.type() == AzureOperation.Type.ACTION) {
                    break;
                }
            }
            current = current.getParent();
        }
        return result;
    }

    @Nonnull
    private static List<IAzureOperation> getExceptionOperations(@Nonnull Throwable throwable) {
        return ExceptionUtils.getThrowableList(throwable).stream()
                .filter(object -> object instanceof AzureOperationException)
                .map(o -> ((AzureOperationException) o).getOperation())
                .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public Type getType() {
        return this.original.getType();
    }

    @Nonnull
    @Override
    public String getTitle() {
        return StringUtils.firstNonBlank(this.title, this.original.getTitle(), DEFAULT_MESSAGE_TITLE);
    }

    @Nullable
    @Override
    public Object getPayload() {
        return ObjectUtils.firstNonNull(this.payload, this.original.getPayload());
    }

    @Nonnull
    @Override
    public Action[] getActions() {
        return ObjectUtils.firstNonNull(this.actions, this.original.getActions(), new Action[0]);
    }

    @Nullable
    public Boolean getBackgrounded() {
        Boolean b = this.backgrounded;
        if (Objects.isNull(b)) {
            final AzureTask<?> task = AzureTaskContext.current().getTask();
            b = Optional.ofNullable(task).map(AzureTask::getBackgrounded).orElse(null);
        }
        return b;
    }

    @Nonnull
    public static AzureMessage.Context getContext() {
        return getContext(IAzureOperation.current());
    }

    @Nonnull
    public static AzureMessage.Context getActionContext() {
        final IAzureOperation operation = IAzureOperation.current();
        return Optional.ofNullable(operation)
                .map(IAzureOperation::getActionParent)
                .map(AzureMessage::getContext)
                .orElse(new AzureMessage.Context(operation));
    }

    @Nonnull
    public static AzureMessage.Context getContext(@Nullable IAzureOperation operation) {
        return Optional.ofNullable(operation)
                .map(o -> o.get(AzureMessage.Context.class, new AzureMessage.Context(operation)))
                .orElse(new AzureMessage.Context(operation));
    }

    @RequiredArgsConstructor
    public static class Context implements IAzureOperation.IContext {
        @Nullable
        private final IAzureOperation operation;
        private IAzureMessager messager = null;
        private final Map<String, Object> properties = new HashMap<>();

        public void setProperty(@Nonnull String key, Object val) {
            this.properties.put(key, val);
        }

        public Object getProperty(@Nonnull String key) {
            return this.properties.get(key);
        }

        public void setMessager(@Nonnull IAzureMessager messager) {
            this.messager = messager;
        }

        @Nonnull
        public IAzureMessager getMessager() {
            if (Objects.nonNull(this.messager)) {
                return messager;
            }
            return getMessager(Optional.ofNullable(this.operation).map(IAzureOperation::getParent).orElse(null));
        }

        @Nonnull
        public IAzureMessager getActionMessager() {
            return getMessager(Optional.ofNullable(this.operation).map(IAzureOperation::getActionParent).orElse(null));
        }

        @Nonnull
        private IAzureMessager getMessager(@Nullable IAzureOperation op) {
            return Optional.ofNullable(op)
                    .map(AzureMessage::getContext)
                    .map(Context::getMessager)
                    .orElse(AzureMessager.getDefaultMessager());
        }
    }
}
