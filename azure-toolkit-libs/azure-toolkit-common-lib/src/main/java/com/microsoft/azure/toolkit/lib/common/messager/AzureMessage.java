/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationException;
import com.microsoft.azure.toolkit.lib.common.operation.OperationThreadContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Accessors(chain = true)
@Getter
@Setter
public class AzureMessage implements IAzureMessage {
    @Nonnull
    protected final Type type;
    @Nonnull
    protected final AzureString message;
    @Nullable
    protected String title;
    @Nullable
    protected Object payload;
    @Nullable
    protected Object[] actions;
    protected ValueDecorator valueDecorator;

    @Nonnull
    public String getContent() {
        if (!(getPayload() instanceof Throwable)) {
            return ObjectUtils.firstNonNull(this.decorateText(this.message, null), this.message.getString());
        }
        final Throwable throwable = (Throwable) getPayload();
        final List<Operation> operations = this.getOperations();
        final String failure = operations.stream().findFirst().map(Operation::getDescription)
            .map(azureString -> "Failed to " + this.decorateText(azureString, azureString::getString)).orElse("Failed to proceed");
        final String cause = Optional.ofNullable(this.getCause(throwable)).map(c -> ", " + c).orElse("");
        final String tips = Optional.ofNullable(this.getExceptionTips(throwable)).map(c -> System.lineSeparator() + c).orElse("");
        return failure + cause + tips;
    }

    public String getDetails() {
        final List<Operation> operations = this.getOperations();
        return getPayload() instanceof Throwable && operations.size() < 2 ? "" : operations.stream()
            .map(this::getDetailItem)
            .filter(StringUtils::isNoneBlank)
            .collect(Collectors.joining("", "", ""));
    }

    protected String getDetailItem(Operation o) {
        return Optional.ofNullable(o.getDescription())
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
        final Throwable cause = ExceptionUtils.getRootCause(throwable);
        AtomicReference<String> causeMessage = new AtomicReference<>(null);
        if (cause instanceof ManagementException) {
            causeMessage.set(Optional.of((ManagementException) cause)
                    .map(ManagementException::getValue)
                    .map(ManagementError::getMessage)
                    .orElse("Unknown cause"));
        } else if (cause instanceof HttpResponseException) {
            Optional.of((HttpResponseException) cause)
                .map(HttpResponseException::getResponse)
                .map(HttpResponse::getBodyAsString)
                .orElse(Mono.just("Unknown cause"))
                .subscribe(causeMessage::set);
        }
        final String causeMsg = StringUtils.firstNonBlank(causeMessage.get(), cause.getMessage());
        return Optional.ofNullable(causeMsg)
            .filter(StringUtils::isNotBlank)
            .map(StringUtils::uncapitalize)
            .map(c -> c.endsWith(".") ? c : c + '.')
            .orElse(null);
    }

    @Nullable
    private static Throwable getRecognizableCause(@Nonnull Throwable throwable) {
        final List<Throwable> throwables = ExceptionUtils.getThrowableList(throwable);
        for (int i = throwables.size() - 1; i >= 0; i--) {
            final Throwable t = throwables.get(i);
            if (t instanceof OperationException) {
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
    protected String getExceptionTips(@Nonnull Throwable throwable) {
        return ExceptionUtils.getThrowableList(throwable).stream()
            .filter(t -> t instanceof AzureToolkitRuntimeException || t instanceof AzureToolkitException)
            .map(t -> t instanceof AzureToolkitRuntimeException ? ((AzureToolkitRuntimeException) t).getTips() : ((AzureToolkitException) t).getTips())
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .map(StringUtils::capitalize)
            .orElse(null);
    }

    @Nonnull
    protected List<Operation> getOperations() {
        final List<Operation> exceptionOperations = Optional.ofNullable(this.getPayload())
            .filter(p -> p instanceof Throwable)
            .map(p -> getExceptionOperations((Throwable) p))
            .orElse(new ArrayList<>());
        final Operation current = exceptionOperations.isEmpty() ? OperationThreadContext.current().currentOperation() : exceptionOperations.get(0);
        final List<Operation> contextOperations = getAncestorOperationsUtilAction(current);
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        final List<Operation> operations = Streams.concat(contextOperations.stream(), exceptionOperations.stream())
            .filter(t -> seen.add(t.getId()))
            .filter(o -> Objects.nonNull(o.getDescription()))
            .collect(Collectors.toList());
        return Lists.reverse(operations);
    }

    @Nonnull
    private static List<Operation> getAncestorOperationsUtilAction(Operation current) {
        final LinkedList<Operation> result = new LinkedList<>();
        while (Objects.nonNull(current)) {
            result.addFirst(current);
            if (StringUtils.equalsAnyIgnoreCase(current.getType(), Operation.Type.USER, Operation.Type.PLATFORM)) {
                break;
            }
            current = current.getParent();
        }
        return result;
    }

    @Nonnull
    private static List<Operation> getExceptionOperations(@Nonnull Throwable throwable) {
        return ExceptionUtils.getThrowableList(throwable).stream()
            .filter(object -> object instanceof OperationException)
            .map(o -> ((OperationException) o).getOperation())
            .collect(Collectors.toList());
    }

    @Nonnull
    private static List<Object> getExceptionActions(@Nonnull Throwable throwable) {
        return ExceptionUtils.getThrowableList(throwable).stream()
            .filter(object -> object instanceof AzureToolkitRuntimeException || object instanceof AzureToolkitException)
            .flatMap(o -> {
                final Object[] actions = o instanceof AzureToolkitRuntimeException ?
                    ((AzureToolkitRuntimeException) o).getActions() : ((AzureToolkitException) o).getActions();
                return Arrays.stream(ObjectUtils.firstNonNull(actions, new Object[0]));
            }).collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public String getTitle() {
        return ObjectUtils.firstNonNull(this.title, "");
    }

    @Nonnull
    @Override
    public Action<?>[] getActions() {
        final Object payload = this.getPayload();
        final List<Object> actions = new ArrayList<>(Arrays.asList(ObjectUtils.firstNonNull(this.actions, new Object[0])));
        if (payload instanceof Throwable) {
            actions.addAll(getExceptionActions((Throwable) payload));
        }
        return actions.stream()
            .filter(ObjectUtils::isNotEmpty)
            .map(this::toAction)
            .filter(Objects::nonNull)
            .distinct()
            .toArray(Action<?>[]::new);
    }

    public AzureMessage setActions(Object[] actions) {
        this.actions = actions;
        return this;
    }

    private Action<?> toAction(Object action) {
        final AzureActionManager am = AzureActionManager.getInstance();
        try {
            if (action instanceof String) {
                final Action.Id<?> id = Action.Id.of(((String) action));
                return am.getAction(id);
            } else if (action instanceof Action.Id) {
                return am.getAction(((Action.Id<?>) action));
            }
            return (Action<?>) action;
        } catch (Throwable t) {
            return null;
        }
    }
}
