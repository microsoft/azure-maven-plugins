/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBase;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Accessors(chain = true)
public class Action<D> extends OperationBase {
    public static final String SOURCE = "ACTION_SOURCE";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final Id<Runnable> REQUIRE_AUTH = Id.of("common.requireAuth");
    public static final Id<Object> AUTHENTICATE = Id.of("account.authenticate");
    @Nonnull
    private final List<AbstractMap.SimpleEntry<BiPredicate<D, ?>, BiConsumer<D, ?>>> handlers = new ArrayList<>();
    @Nonnull
    private final Id<D> id;

    @Nullable
    @Getter
    private ActionView.Builder viewBuilder;
    @Setter
    @Getter
    private boolean authRequired = true;
    /**
     * shortcuts for this action.
     * 1. directly bound to this action if it's IDE-specific type of shortcuts (e.g. {@code ShortcutSet} in IntelliJ).
     * 2. interpreted into native shortcuts first and then bound to this action if it's {@code String[]/String} (e.g. {@code "alt X"}).
     * 3. copy shortcuts from actions specified by this action id and then bound to this action if it's {@link Id} of another action.
     */
    @Setter
    @Getter
    private Object shortcuts;

    public Action(@Nonnull final Id<D> id, @Nullable ActionView.Builder viewBuilder) {
        this.id = id;
        this.viewBuilder = viewBuilder;
    }

    public Action(@Nonnull final Id<D> id, @Nonnull Consumer<D> handler) {
        this.id = id;
        this.registerHandler((d, e) -> true, (d, e) -> handler.accept(d));
    }

    public <E> Action(@Nonnull final Id<D> id, @Nonnull BiConsumer<D, E> handler) {
        this.id = id;
        this.registerHandler((d, e) -> true, handler);
    }

    public Action(@Nonnull final Id<D> id, @Nonnull Consumer<D> handler, @Nullable ActionView.Builder viewBuilder) {
        this.id = id;
        this.viewBuilder = viewBuilder;
        this.registerHandler((d, e) -> true, (d, e) -> handler.accept(d));
    }

    public <E> Action(@Nonnull final Id<D> id, @Nonnull BiConsumer<D, E> handler, @Nullable ActionView.Builder viewBuilder) {
        this.id = id;
        this.viewBuilder = viewBuilder;
        this.registerHandler((d, e) -> true, handler);
    }

    @Nonnull
    @Override
    public String getId() {
        return this.id.id;
    }

    @Nullable
    public IView.Label getView(D source) {
        return Objects.nonNull(this.viewBuilder) ? this.viewBuilder.toActionView(source) : null;
    }

    @SuppressWarnings("unchecked")
    public BiConsumer<D, Object> getHandler(D source, Object e) {
        for (int i = this.handlers.size() - 1; i >= 0; i--) {
            final AbstractMap.SimpleEntry<BiPredicate<D, ?>, BiConsumer<D, ?>> p = this.handlers.get(i);
            final BiPredicate<D, Object> condition = (BiPredicate<D, Object>) p.getKey();
            final BiConsumer<D, Object> handler = (BiConsumer<D, Object>) p.getValue();
            if (condition.test(source, e)) {
                return handler;
            }
        }
        return null;
    }

    public void handle(D source, Object e) {
        final Runnable runnable = () -> {
            final BiConsumer<D, Object> handler = this.getHandler(source, e);
            if (Objects.nonNull(handler)) {
                final AzureString title = Optional.ofNullable(this.viewBuilder).map(b -> b.title).map(t -> t.apply(source))
                    .orElse(AzureString.fromString(Operation.UNKNOWN_NAME));
                final AzureTask<Void> task = new AzureTask<>(title, () -> handle(source, e, handler));
                task.setType(AzureOperation.Type.ACTION.name());
                AzureTaskManager.getInstance().runInBackground(task);
            }
        };
        if (this.authRequired) {
            final Action<Runnable> requireAuth = AzureActionManager.getInstance().getAction(REQUIRE_AUTH);
            if (Objects.nonNull(requireAuth)) {
                requireAuth.handle(runnable, e);
            }
        } else {
            runnable.run();
        }
    }

    protected void handle(D source, Object e, BiConsumer<D, Object> handler) {
        if (source instanceof AzResource) {
            final AzResource<?, ?> resource = (AzResource<?, ?>) source;
            final OperationContext context = OperationContext.action();
            context.setTelemetryProperty("subscriptionId", resource.getSubscriptionId());
            context.setTelemetryProperty("resourceType", resource.getFullResourceType());
        } else if (source instanceof AzResourceModule) {
            final AzResourceModule<?, ?> resource = (AzResourceModule<?, ?>) source;
            final OperationContext context = OperationContext.action();
            context.setTelemetryProperty("subscriptionId", resource.getSubscriptionId());
            context.setTelemetryProperty("resourceType", resource.getFullResourceType());
        }
        handler.accept(source, e);
    }

    public void handle(D source) {
        this.handle(source, null);
    }

    public void registerHandler(@Nonnull Predicate<D> condition, @Nonnull Consumer<D> handler) {
        this.handlers.add(new AbstractMap.SimpleEntry<>((d, e) -> condition.test(d), (d, e) -> handler.accept(d)));
    }

    public <E> void registerHandler(@Nonnull BiPredicate<D, E> condition, @Nonnull BiConsumer<D, E> handler) {
        this.handlers.add(new AbstractMap.SimpleEntry<>(condition, handler));
    }

    @Override
    public Callable<?> getBody() {
        throw new AzureToolkitRuntimeException("'action.getBody()' is not supported");
    }

    @Nonnull
    @Override
    public String getType() {
        return AzureOperation.Type.ACTION.name();
    }

    @Nullable
    @Override
    public AzureString getDescription() {
        return OperationBundle.description(this.id.id);
    }

    public static class Id<D> {
        @Nonnull
        private final String id;

        private Id(@Nonnull String id) {
            this.id = id;
        }

        public static <D> Id<D> of(@PropertyKey(resourceBundle = OperationBundle.BUNDLE) @Nonnull String id) {
            assert StringUtils.isNotBlank(id) : "action id can not be blank";
            return new Id<>(id);
        }

        @Nonnull
        public String getId() {
            return id;
        }
    }

    public static Action<Void> retryFromFailure(@Nonnull Runnable handler) {
        return new Action<>(Id.of("common.retry"), (v) -> handler.run(), new ActionView.Builder("Retry"));
    }
}

