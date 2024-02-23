package com.microsoft.azure.toolkit.lib.common.action;

import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationAspect;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBase;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

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

import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.REQUIRE_AUTH;

@Slf4j
public class ActionInstance<D> extends OperationBase {
    @Getter
    public final Action<D> action;
    @Getter
    @Nullable
    private final D target;
    @Getter
    @Nullable
    private final Object event;

    private Function<D, String> iconProvider;
    private Function<D, String> labelProvider;
    private Function<D, AzureString> titleProvider;
    private Predicate<D> authRequiredProvider;
    private final List<Function<D, String>> titleParamProviders = new ArrayList<>();

    public ActionInstance(@Nonnull final Action<D> action, @Nullable final D target, @Nullable final Object event) {
        super();
        this.action = action;
        this.target = target;
        this.event = event;
    }

    @Nonnull
    @Override
    public String getId() {
        return action.getId().getId();
    }

    public String getPlace() {
        return AzureActionManager.getInstance().getPlace(this);
    }

    @Override
    public Callable<?> getBody() {
        return () -> {
            final BiConsumer<D, Object> handler = this.action.getHandler(target, event);
            Optional.ofNullable(handler).ifPresent(h -> h.accept(target, event));
            //noinspection ReturnOfNull
            return null;
        };
    }

    public boolean isPerformable() {
        return Objects.nonNull(this.action.getHandler(target, event));
    }

    @Nullable
    @Override
    public Object getSource() {
        return Optional.ofNullable(this.action.sourceProvider).map(p -> p.apply(this.target)).orElse(this.target);
    }

    @Nullable
    @Override
    public AzureString getDescription() {
        final Function<D, AzureString> titleProvider = Optional.ofNullable(this.titleProvider).orElse(this.action.titleProvider);
        final List<Function<D, String>> titleParamProviders = Optional.of(this.titleParamProviders).filter(CollectionUtils::isNotEmpty).orElse(this.action.titleParamProviders);
        if (Objects.nonNull(titleProvider)) {
            return titleProvider.apply(this.target);
        } else {
            if (!titleParamProviders.isEmpty()) {
                final Object[] params = titleParamProviders.stream().map(p -> p.apply(this.target)).toArray();
                return OperationBundle.description(this.action.getId().getId(), params);
            }
        }
        return OperationBundle.description(this.action.getId().getId());
    }

    @Nonnull
    public IView.Label getView() {
        return getView(this.getPlace());
    }

    @Nonnull
    public IView.Label getView(String place) {
        try {
            final Predicate<D> enableWhen = this.action.enableWhen;
            final BiPredicate<Object, String> visibleWhen = this.action.visibleWhen;
            final Function<D, String> labelProvider = Optional.ofNullable(this.labelProvider).orElse(this.action.labelProvider);
            final Function<D, String> iconProvider = Optional.ofNullable(this.iconProvider).orElse(this.action.iconProvider);

            final boolean visible = visibleWhen.test(this.target, place);
            if (visible) {
                final String label = labelProvider.apply(this.target);
                final String icon = Optional.ofNullable(iconProvider).map(p -> p.apply(this.target)).orElse(null);
                final boolean enabled = enableWhen.test(this.target) && isPerformable();
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
        return Optional.ofNullable(authRequiredProvider).map(p -> p.test(this.target)).orElse(true);
    }

    @SneakyThrows
    public void perform() {
        if (isAuthRequired() && !Azure.az(IAzureAccount.class).isLoggedIn()) {
            final SettableFuture<IAccount> authorized = SettableFuture.create();
            this.getContext().setTelemetryProperty(PLACE, this.getPlace());
            final Action<Consumer<IAccount>> authorizeAction = AzureActionManager.getInstance().getAction(REQUIRE_AUTH);
            authorizeAction.handleSync(authorized::set, this.event);
            if (Objects.isNull(authorized.get())) {
                Azure.az(IAzureAccount.class).account();
            }
        }
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

