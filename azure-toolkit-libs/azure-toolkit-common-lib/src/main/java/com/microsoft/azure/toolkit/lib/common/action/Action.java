/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.Emulatable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationAspect;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBase;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"unused", "UnresolvedPropertyKey"})
@Slf4j
@Accessors(chain = true)
public class Action<D> {
    public static final String SOURCE = "ACTION_SOURCE";
    public static final String PLACE = "action_place";
    public static final String EMPTY_PLACE = "empty";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final Id<Object> AUTHENTICATE = Id.of("user/account.authenticate");
    public static final Id<Runnable> REQUIRE_AUTH = Id.of("user/common.requireAuth");
    public static final Action.Id<Object> OPEN_AZURE_SETTINGS = Action.Id.of("user/common.open_azure_settings");
    public static final Action.Id<Object> DISABLE_AUTH_CACHE = Action.Id.of("user/account.disable_auth_cache");

    public static final String COMMON_PLACE = "common";

    @Getter
    @Nonnull
    private final Id<D> id;
    @Nonnull
    private Predicate<D> enableWhen = o -> true;
    private BiPredicate<Object, String> visibleWhen = (o, place) -> true;
    private Function<D, String> iconProvider;
    private Function<D, String> labelProvider;
    private Function<D, AzureString> titleProvider;
    @Nonnull
    private List<Pair<BiPredicate<D, ?>, BiConsumer<D, ?>>> handlers = new ArrayList<>();
    @Nonnull
    private List<Function<D, String>> titleParamProviders = new ArrayList<>();
    private D source;

    @Setter
    private Predicate<D> authRequiredProvider = Action::isAuthRequiredForAzureResource;
    /**
     * shortcuts for this action.
     * 1. directly bound to this action if it's IDE-specific type of shortcuts (e.g. {@code ShortcutSet} in IntelliJ).
     * 2. interpreted into native shortcuts first and then bound to this action if it's {@code String[]/String} (e.g. {@code "alt X"}).
     * 3. copy shortcuts from actions specified by this action id and then bound to this action if it's {@link Id} of another action.
     */
    @Setter
    @Getter
    private Object shortcut;

    public Action(@Nonnull Id<D> id) {
        this.id = id;
    }

    public IView.Label getView(D s) {
        return getView(s, COMMON_PLACE);
    }

    @Nonnull
    public IView.Label getView(D s, final String place) {
        final ActionInstance instance = this.instantiate(s, null);
        return Optional.ofNullable(instance).map(i -> i.getView(place)).orElse(View.INVISIBLE);
    }

    /**
     * perform asynchronously
     */
    public void handle(D s) {
        this.handle(s, null);
    }

    /**
     * perform asynchronously
     */
    public void handle(D s, Object e) {
        final ActionInstance instance = this.instantiate(s, e);
        Optional.ofNullable(instance).ifPresent(ActionInstance::performAsync);
    }

    public Action<D> enableWhen(@Nonnull Predicate<D> enableWhen) {
        this.enableWhen = enableWhen;
        return this;
    }

    public Action<D> visibleWhen(@Nonnull Predicate<Object> visibleWhen) {
        this.visibleWhen = (object, ignore) -> visibleWhen.test(object);
        return this;
    }

    public Action<D> visibleWhen(@Nonnull BiPredicate<Object, String> visibleWhen) {
        this.visibleWhen = visibleWhen;
        return this;
    }

    public Action<D> withLabel(@Nonnull final String label) {
        this.labelProvider = (any) -> label;
        return this;
    }

    public Action<D> withLabel(@Nonnull final Function<D, String> labelProvider) {
        this.labelProvider = labelProvider;
        return this;
    }

    public Action<D> withIcon(@Nonnull final String icon) {
        this.iconProvider = (any) -> icon;
        return this;
    }

    public Action<D> withIcon(@Nonnull final Function<D, String> iconProvider) {
        this.iconProvider = iconProvider;
        return this;
    }

    public Action<D> withTitle(@Nonnull final AzureString title) {
        this.titleProvider = (any) -> title;
        return this;
    }

    public Action<D> withShortcut(@Nonnull final Object shortcut) {
        this.shortcut = shortcut;
        return this;
    }

    public Action<D> withHandler(@Nonnull Consumer<D> handler) {
        this.handlers.add(Pair.of((d, e) -> true, (d, e) -> handler.accept(d)));
        return this;
    }

    public <E> Action<D> withHandler(@Nonnull BiConsumer<D, E> handler) {
        this.handlers.add(Pair.of((d, e) -> true, handler));
        return this;
    }

    public Action<D> withHandler(@Nonnull Predicate<D> condition, @Nonnull Consumer<D> handler) {
        this.handlers.add(Pair.of((d, e) -> condition.test(d), (d, e) -> handler.accept(d)));
        return this;
    }

    public <E> Action<D> withHandler(@Nonnull BiPredicate<D, E> condition, @Nonnull BiConsumer<D, E> handler) {
        this.handlers.add(Pair.of(condition, handler));
        return this;
    }

    public Action<D> withAuthRequired(boolean authRequired) {
        this.authRequiredProvider = ignore -> authRequired;
        return this;
    }

    public Action<D> withAuthRequired(@Nonnull Predicate<D> authRequiredProvider) {
        this.authRequiredProvider = authRequiredProvider;
        return this;
    }

    public Action<D> withIdParam(@Nonnull final String titleParam) {
        this.titleParamProviders.add((d) -> titleParam);
        return this;
    }

    public Action<D> withIdParam(@Nonnull final Function<D, String> titleParamProvider) {
        this.titleParamProviders.add(titleParamProvider);
        return this;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private BiConsumer<D, Object> getHandler(D s, Object e) {
        if (!this.visibleWhen.test(s, COMMON_PLACE) && !this.enableWhen.test(s)) {
            return null;
        }
        for (int i = this.handlers.size() - 1; i >= 0; i--) {
            final Pair<BiPredicate<D, ?>, BiConsumer<D, ?>> p = this.handlers.get(i);
            final BiPredicate<D, Object> condition = (BiPredicate<D, Object>) p.getKey();
            final BiConsumer<D, Object> handler = (BiConsumer<D, Object>) p.getValue();
            if (condition.test(s, e)) {
                return handler;
            }
        }
        return null;
    }

    public Action<D> bind(D s) {
        try {
            // noinspection unchecked
            final Action<D> clone = (Action<D>) this.clone();
            clone.handlers = new ArrayList<>(this.handlers);
            clone.titleParamProviders = new ArrayList<>(this.titleParamProviders);
            clone.source = s;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public ActionInstance instantiate(D s, Object event) {
        s = Optional.ofNullable(this.source).orElse(s);
        final BiConsumer<D, Object> handler = getHandler(s, event);
        if (Objects.isNull(handler)) {
            return null;
        }
        return new ActionInstance(s, event, handler);
    }

    public void register(AzureActionManager am) {
        am.registerAction(this);
    }

    @Override
    public String toString() {
        return String.format("Action {id:'%s', bindTo: %s}", this.getId(), this.source);
    }

    @RequiredArgsConstructor
    public class ActionInstance extends OperationBase {
        @Nullable
        private final D source;
        @Nullable
        private final Object event;
        @Nonnull
        private final BiConsumer<D, Object> handler;

        @Nonnull
        @Override
        public String getId() {
            return id.id;
        }

        @Override
        public Callable<?> getBody() {
            return () -> {
                final Runnable handlerBody = () -> this.handler.accept(this.source, this.event);
                if (isAuthRequired(this.source)) {
                    final Action<Runnable> requireAuth = AzureActionManager.getInstance().getAction(REQUIRE_AUTH);
                    if (Objects.nonNull(requireAuth)) {
                        requireAuth.handle(handlerBody, this.source);
                    }
                } else {
                    handlerBody.run();
                }
                return null;
            };
        }

        @Nullable
        @Override
        public AzureString getDescription() {
            if (Objects.nonNull(titleProvider)) {
                return titleProvider.apply(this.source);
            } else if (!titleParamProviders.isEmpty()) {
                final Object[] params = titleParamProviders.stream().map(p -> p.apply(this.source)).toArray();
                return OperationBundle.description(id.id, params);
            }
            return OperationBundle.description(id.id);
        }

        @Nonnull
        public IView.Label getView() {
            return getView(COMMON_PLACE);
        }

        @Nonnull
        public IView.Label getView(String place) {
            try {
                final boolean visible = visibleWhen.test(this.source, place);
                if (visible) {
                    final String label = labelProvider.apply(this.source);
                    final String icon = Optional.ofNullable(iconProvider).map(p -> p.apply(this.source)).orElse(null);
                    final boolean enabled = enableWhen.test(this.source);
                    final AzureString title = getDescription();
                    return new View(label, icon, enabled, title);
                }
                return View.INVISIBLE;
            } catch (final Exception e) {
                log.warn(e.getMessage(), e);
                return View.INVISIBLE;
            }
        }

        private boolean isAuthRequired(D s) {
            return Optional.ofNullable(authRequiredProvider).map(p -> p.test(this.source)).orElse(true);
        }

        @SneakyThrows
        public void perform() {
            AzureOperationAspect.execute(this, this.source);
        }

        public void performAsync() {
            AzureTaskManager.getInstance().runInBackground(this.getDescription(), this::perform);
        }
    }

    @Getter
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

        public String toString() {
            return id;
        }
    }

    @Getter
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class View implements IView.Label {
        public static View INVISIBLE = new View("", "", false, false, null);
        @Nonnull
        private final String label;
        private final String iconPath;
        private final boolean enabled;
        private boolean visible = true;
        @Nullable
        private AzureString title;

        public View(@Nonnull String label, String iconPath, boolean enabled, @Nullable AzureString title) {
            this(label, iconPath, enabled, true, title);
        }

        @Override
        public String getDescription() {
            return Optional.ofNullable(this.title).map(AzureString::toString).orElse(null);
        }

        @Override
        public void dispose() {
        }
    }

    public static Action<Void> retryFromFailure(@Nonnull Runnable handler) {
        return new Action<>(Id.<Void>of("common.retry"))
            .withHandler((v) -> handler.run())
            .withLabel("Retry");
    }

    public static <D> Boolean isAuthRequiredForAzureResource(@Nullable final D resource) {
        return !(resource instanceof Emulatable && ((Emulatable) resource).isEmulatorResource());
    }
}

