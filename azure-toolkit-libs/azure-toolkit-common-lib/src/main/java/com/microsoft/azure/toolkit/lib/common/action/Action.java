/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Emulatable;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBase;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
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

@SuppressWarnings("unused")
@Accessors(chain = true)
public class Action<D> extends OperationBase implements Cloneable {
    public static final String SOURCE = "ACTION_SOURCE";
    public static final String PLACE = "action_place";
    public static final String EMPTY_PLACE = "empty";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final Id<Object> AUTHENTICATE = Id.of("user/account.authenticate");
    public static final Id<Runnable> REQUIRE_AUTH = Id.of("user/common.requireAuth");
    public static final Action.Id<Object> OPEN_AZURE_SETTINGS = Action.Id.of("user/common.open_azure_settings");
    public static final Action.Id<Object> DISABLE_AUTH_CACHE = Action.Id.of("user/account.disable_auth_cache");

    public static final String COMMON = "common";

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
    private String place;
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

    @Nonnull
    @Override
    public String getId() {
        return this.id.id;
    }

    public IView.Label getView(D s) {
        return getView(s, COMMON);
    }

    @Nonnull
    public IView.Label getView(D s, final String place) {
        final D source = Optional.ofNullable(this.source).orElse(s);
        try {
            final boolean visible = this.visibleWhen.test(source, place);
            if (visible) {
                final String label = this.labelProvider.apply(source);
                final String icon = Optional.ofNullable(this.iconProvider).map(p -> p.apply(source)).orElse(null);
                final boolean enabled = this.enableWhen.test(source);
                final AzureString title = this.getTitle(source);
                return new View(label, icon, enabled, title);
            }
            return View.INVISIBLE;
        } catch (final Exception e) {
            e.printStackTrace();
            return View.INVISIBLE;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public BiConsumer<D, Object> getHandler(D s, Object e) {
        final D source = Optional.ofNullable(this.source).orElse(s);
        if (!this.visibleWhen.test(source, COMMON) && !this.enableWhen.test(source)) {
            return null;
        }
        for (int i = this.handlers.size() - 1; i >= 0; i--) {
            final Pair<BiPredicate<D, ?>, BiConsumer<D, ?>> p = this.handlers.get(i);
            final BiPredicate<D, Object> condition = (BiPredicate<D, Object>) p.getKey();
            final BiConsumer<D, Object> handler = (BiConsumer<D, Object>) p.getValue();
            if (condition.test(source, e)) {
                return handler;
            }
        }
        return null;
    }

    public void handle(D s, Object e) {
        final D source = Optional.ofNullable(this.source).orElse(s);
        final BiConsumer<D, Object> handler = this.getHandler(source, e);
        if (Objects.isNull(handler)) {
            return;
        }
        final AzureString title = this.getTitle(source);
        final Runnable operationBody = () -> AzureTaskManager.getInstance().runInBackground(title, () -> handle(source, e, handler));
        if (this.isAuthRequired(s)) {
            final Action<Runnable> requireAuth = AzureActionManager.getInstance().getAction(REQUIRE_AUTH);
            if (Objects.nonNull(requireAuth)) {
                requireAuth.handle(operationBody, e);
            }
        } else {
            operationBody.run();
        }
    }

    private void handle(D s, Object e, BiConsumer<D, Object> handler) {
        final D source = Optional.ofNullable(this.source).orElse(s);
        final OperationContext context = OperationContext.action();
        context.setTelemetryProperties(this.getContext().getTelemetryProperties());
        if (source instanceof AzResource) {
            final AzResource resource = (AzResource) source;
            context.setTelemetryProperty("subscriptionId", resource.getSubscriptionId());
            context.setTelemetryProperty("resourceType", resource.getFullResourceType());
        } else if (source instanceof AzResourceModule) {
            final AzResourceModule<?> resource = (AzResourceModule<?>) source;
            context.setTelemetryProperty("subscriptionId", resource.getSubscriptionId());
            context.setTelemetryProperty("resourceType", resource.getFullResourceType());
        }
        handler.accept(source, e);
    }

    public void handle(D s) {
        final D source = Optional.ofNullable(this.source).orElse(s);
        this.handle(source, null);
    }

    @Override
    public Callable<?> getBody() {
        throw new AzureToolkitRuntimeException("'action.getBody()' is not supported");
    }

    @Nonnull
    @Override
    public String getType() {
        return Type.USER;
    }

    private boolean isAuthRequired(D s) {
        final D source = Optional.ofNullable(this.source).orElse(s);
        return Optional.ofNullable(this.authRequiredProvider).map(p -> p.test(source)).orElse(true);
    }

    public AzureString getTitle(D s) {
        final D source = Optional.ofNullable(this.source).orElse(s);
        if (Objects.nonNull(this.titleProvider)) {
            return this.titleProvider.apply(source);
        } else if (!this.titleParamProviders.isEmpty()) {
            final Object[] params = this.titleParamProviders.stream().map(p -> p.apply(source)).toArray();
            return OperationBundle.description(this.id.id, params);
        }
        return this.getDescription();
    }

    @Nonnull
    @Override
    public AzureString getDescription() {
        return OperationBundle.description(this.id.id);
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

    public Action<D> bind(D source) {
        try {
            // noinspection unchecked
            final Action<D> clone = (Action<D>) this.clone();
            clone.handlers = new ArrayList<>(this.handlers);
            clone.titleParamProviders = new ArrayList<>(this.titleParamProviders);
            clone.source = source;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void register(AzureActionManager am) {
        am.registerAction(this);
    }

    @Override
    public String toString() {
        return String.format("Action {id:'%s', bindTo: %s}", this.getId(), this.source);
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

