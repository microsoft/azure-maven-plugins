/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Accessors(chain = true, fluent = true)
public class Action<D> {
    public static final String SOURCE = "ACTION_SOURCE";
    public static final Id<Runnable> REQUIRE_AUTH = Id.of("action.common.requireAuth");
    @Nonnull
    private List<AbstractMap.SimpleEntry<BiPredicate<D, ?>, BiConsumer<D, ?>>> handlers = new ArrayList<>();
    @Nullable
    @Getter
    private ActionView.Builder view;
    @Setter
    private boolean authRequired = true;

    public Action(@Nullable ActionView.Builder view) {
        this.view = view;
    }

    public Action(@Nonnull Consumer<D> handler) {
        this.registerHandler((d, e) -> true, (d, e) -> handler.accept(d));
    }

    public <E> Action(@Nonnull BiConsumer<D, E> handler) {
        this.registerHandler((d, e) -> true, handler);
    }

    public Action(@Nonnull Consumer<D> handler, @Nullable ActionView.Builder view) {
        this.view = view;
        this.registerHandler((d, e) -> true, (d, e) -> handler.accept(d));
    }

    public <E> Action(@Nonnull BiConsumer<D, E> handler, @Nullable ActionView.Builder view) {
        this.view = view;
        this.registerHandler((d, e) -> true, handler);
    }

    private Action(@Nonnull List<AbstractMap.SimpleEntry<BiPredicate<D, ?>, BiConsumer<D, ?>>> handlers, @Nullable ActionView.Builder view) {
        this.view = view;
        this.handlers = handlers;
    }

    @Nullable
    public IView.Label view(D source) {
        return Objects.nonNull(this.view) ? this.view.toActionView(source) : null;
    }

    @SuppressWarnings("unchecked")
    public BiConsumer<D, Object> handler(D source, Object e) {
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
            final BiConsumer<D, Object> handler = this.handler(source, e);
            if (Objects.nonNull(handler)) {
                final AzureString title = Optional.ofNullable(this.view).map(b -> b.title).map(t -> t.apply(source))
                        .orElse(AzureString.fromString(IAzureOperation.UNKNOWN_NAME));
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
        if (source instanceof IAzureResource) {
            AzureTelemetry.getActionContext().setProperty("subscriptionId", ((IAzureResource<?>) source).subscriptionId());
            AzureTelemetry.getActionContext().setProperty("resourceType", source.getClass().getSimpleName());
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

    public static class Id<D> {
        @Nonnull
        private final String id;

        private Id(@Nonnull String id) {
            this.id = id;
        }

        public static <D> Id<D> of(@Nonnull String id) {
            assert StringUtils.isNotBlank(id) : "action id can not be blank";
            return new Id<>(id);
        }

        @Nonnull
        public String getId() {
            return id;
        }
    }
}

