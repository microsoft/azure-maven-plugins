package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationAspect;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBase;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.microsoft.azure.toolkit.lib.common.action.Action.COMMON_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.REQUIRE_AUTH;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class ActionInstance<D> extends OperationBase {
    @EqualsAndHashCode.Include
    public final Action<D> action;
    @Getter
    @Nullable
    @EqualsAndHashCode.Include
    private final D source;
    @Nullable
    private final Object event;
    @Nonnull
    private final BiConsumer<D, Object> handler;

    private Function<D, String> iconProvider;
    private Function<D, String> labelProvider;
    private Function<D, AzureString> titleProvider;
    private Predicate<D> authRequiredProvider;
    private List<Function<D, String>> titleParamProviders;

    @Nonnull
    @Override
    public String getId() {
        return action.getId().getId();
    }

    @Override
    public Callable<?> getBody() {
        return () -> {
            final Runnable handlerBody = () -> this.handler.accept(this.source, this.event);
            if (isAuthRequired()) {
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
        final Function<D, AzureString> titleProvider = Optional.ofNullable(this.titleProvider).orElse(this.action.titleProvider);
        final List<Function<D, String>> titleParamProviders = Optional.ofNullable(this.titleParamProviders).orElse(this.action.titleParamProviders);
        if (Objects.nonNull(titleProvider)) {
            return titleProvider.apply(this.source);
        } else {
            if (!titleParamProviders.isEmpty()) {
                final Object[] params = titleParamProviders.stream().map(p -> p.apply(this.source)).toArray();
                return OperationBundle.description(this.action.getId().getId(), params);
            }
        }
        return OperationBundle.description(this.action.getId().getId());
    }

    @Nonnull
    public IView.Label getView() {
        return getView(COMMON_PLACE);
    }

    @Nonnull
    public IView.Label getView(String place) {
        try {
            final Predicate<D> enableWhen = this.action.enableWhen;
            final BiPredicate<Object, String> visibleWhen = this.action.visibleWhen;
            final Function<D, String> labelProvider = Optional.ofNullable(this.labelProvider).orElse(this.action.labelProvider);
            final Function<D, String> iconProvider = Optional.ofNullable(this.iconProvider).orElse(this.action.iconProvider);

            final boolean visible = visibleWhen.test(this.source, place);
            if (visible) {
                final String label = labelProvider.apply(this.source);
                final String icon = Optional.ofNullable(iconProvider).map(p -> p.apply(this.source)).orElse(null);
                final boolean enabled = enableWhen.test(this.source);
                final AzureString title = getDescription();
                return new Action.View(label, icon, enabled, title);
            }
            return Action.View.INVISIBLE;
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
            return Action.View.INVISIBLE;
        }
    }

    private boolean isAuthRequired() {
        final Predicate<D> authRequiredProvider = Optional.ofNullable(this.authRequiredProvider).orElse(this.action.authRequiredProvider);
        return Optional.ofNullable(authRequiredProvider).map(p -> p.test(this.source)).orElse(true);
    }

    @SneakyThrows
    public void perform() {
        AzureOperationAspect.execute(this);
    }

    public void performAsync() {
        AzureTaskManager.getInstance().runInBackground(this.getDescription(), this::perform);
    }

    public ActionInstance<D> withLabel(@Nonnull final String label) {
        this.labelProvider = (any) -> label;
        return this;
    }

    public ActionInstance<D> withLabel(@Nonnull final Function<D, String> labelProvider) {
        this.labelProvider = labelProvider;
        return this;
    }

    public ActionInstance<D> withIcon(@Nonnull final String icon) {
        this.iconProvider = (any) -> icon;
        return this;
    }

    public ActionInstance<D> withIcon(@Nonnull final Function<D, String> iconProvider) {
        this.iconProvider = iconProvider;
        return this;
    }

    public ActionInstance<D> withTitle(@Nonnull final AzureString title) {
        this.titleProvider = (any) -> title;
        return this;
    }

    public ActionInstance<D> withAuthRequired(boolean authRequired) {
        this.authRequiredProvider = ignore -> authRequired;
        return this;
    }

    public ActionInstance<D> withAuthRequired(@Nonnull Predicate<D> authRequiredProvider) {
        this.authRequiredProvider = authRequiredProvider;
        return this;
    }

    public ActionInstance<D> withIdParam(@Nonnull final String titleParam) {
        this.titleParamProviders.add((d) -> titleParam);
        return this;
    }

    public ActionInstance<D> withIdParam(@Nonnull final Function<D, String> titleParamProvider) {
        this.titleParamProviders.add(titleParamProvider);
        return this;
    }
}

