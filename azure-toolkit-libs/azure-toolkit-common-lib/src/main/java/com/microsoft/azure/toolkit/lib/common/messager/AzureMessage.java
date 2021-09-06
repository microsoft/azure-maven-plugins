/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.management.exception.ManagementException;
import com.google.common.collect.Streams;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Accessors(chain = true)
@Getter
@Setter
public class AzureMessage implements IAzureMessage {
    static final String DEFAULT_MESSAGE_TITLE = "Azure";
    @Nonnull
    protected final Type type;
    @Nonnull
    protected final AzureString message;
    @Nullable
    protected String title;
    @Nullable
    protected Object payload;
    @Nullable
    protected Action<?>[] actions;
    protected ValueDecorator valueDecorator;

    @Nonnull
    public String getContent() {
        if (!(getPayload() instanceof Throwable)) {
            return ObjectUtils.firstNonNull(this.decorateText(this.message, null), this.message.getString());
        }
        final Throwable throwable = (Throwable) getPayload();
        final List<IAzureOperation> operations = this.getOperations();
        final String failure = operations.stream().findFirst().map(IAzureOperation::getTitle)
                .map(azureString -> "Failed to " + this.decorateText(azureString, azureString::getString)).orElse("Failed to proceed");
        final String cause = Optional.ofNullable(this.getCause(throwable))
                .map(StringUtils::uncapitalize)
                .map(c -> "," + (c.endsWith(".") ? c : c + '.'))
                .orElse("");
        final String errorAction = Optional.ofNullable(this.getErrorAction(throwable))
                .map(StringUtils::capitalize)
                .map(c -> System.lineSeparator() + (c.endsWith(".") ? c : c + '.'))
                .orElse("");
        return failure + cause + errorAction;
    }

    public String getDetails() {
        final List<IAzureOperation> operations = this.getOperations();
        return operations.size() < 2 ? "" : operations.stream()
                .map(this::getDetailItem)
                .collect(Collectors.joining("", "", ""));
    }

    protected String getDetailItem(IAzureOperation o) {
        return Optional.ofNullable(o.getTitle())
                .map(t -> decorateText(t, t::getString))
                .map(StringUtils::capitalize)
                .map(t -> String.format("● %s", t))
                .orElse(null);
    }

    @Override
    @Nullable
    public String decorateValue(@Nonnull Object p, @Nullable Supplier<String> dft) {
        String result = IAzureMessage.super.decorateValue(p, null);
        if (Objects.isNull(result) && Objects.nonNull(this.valueDecorator)) {
            result = this.valueDecorator.decorateValue(p, this);
        }
        return Objects.isNull(result) && Objects.nonNull(dft) ? dft.get() : result;
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
    @Cacheable(cacheName = "message/operations", key = "${this.hashCode()}")
    protected List<IAzureOperation> getOperations() {
        final List<IAzureOperation> contextOperations = getContextOperations();
        final List<IAzureOperation> exceptionOperations = Optional.ofNullable(this.getPayload())
                .filter(p -> p instanceof Throwable)
                .map(p -> getExceptionOperations((Throwable) p))
                .orElse(new ArrayList<>());
        final Map<String, IAzureOperation> operations = new HashMap<>();
        Streams.concat(contextOperations.stream(), exceptionOperations.stream())
                .filter(o -> !operations.containsKey(o.getName()))
                .forEachOrdered(o -> operations.put(o.getName(), o));
        return new ArrayList<>(operations.values());
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
    public String getTitle() {
        return StringUtils.firstNonBlank(this.title, DEFAULT_MESSAGE_TITLE);
    }

    @Nonnull
    @Override
    public Action<?>[] getActions() {
        return ObjectUtils.firstNonNull(this.actions, new Action[0]);
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
