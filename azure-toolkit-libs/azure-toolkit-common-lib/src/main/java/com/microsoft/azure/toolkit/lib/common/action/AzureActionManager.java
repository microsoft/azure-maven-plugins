/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.view.IView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AzureActionManager {

    public <D> String getPlace(final ActionInstance<D> action) {
        return Action.EMPTY_PLACE;
    }

    private static final class Holder {
        private static final AzureActionManager instance = loadActionManager();

        @Nullable
        private static AzureActionManager loadActionManager() {
            final ClassLoader current = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(AzureActionManager.class.getClassLoader());
                final ServiceLoader<AzureActionManagerProvider> loader = ServiceLoader.load(AzureActionManagerProvider.class, AzureActionManager.class.getClassLoader());
                final Iterator<AzureActionManagerProvider> iterator = loader.iterator();
                if (iterator.hasNext()) {
                    return iterator.next().getActionManager();
                }
                return new DummyActionManager();
            } finally {
                Thread.currentThread().setContextClassLoader(current);
            }
        }
    }

    public static AzureActionManager getInstance() {
        return Holder.instance;
    }

    public abstract <D> void registerAction(Action<D> action);

    public <D> void registerAction(Action.Id<D> id, Consumer<D> action) {
        this.registerAction(new Action<>(id).withHandler(action));
    }

    public abstract <D> Action<D> getAction(Action.Id<D> id);

    public abstract void registerGroup(String id, ActionGroup group);

    public abstract IActionGroup getGroup(String id);

    public <D> void registerHandler(@Nonnull Action.Id<D> id, @Nonnull Predicate<D> condition, @Nonnull Consumer<D> handler) {
        final Action<D> action = this.getAction(id);
        action.withHandler(condition, handler);
    }

    public <D> void registerHandler(@Nonnull Action.Id<D> id, @Nonnull Consumer<D> handler) {
        final Action<D> action = this.getAction(id);
        action.withHandler(o -> true, handler);
    }

    public <D, E> void registerHandler(@Nonnull Action.Id<D> id, @Nonnull BiPredicate<D, E> condition, @Nonnull BiConsumer<D, E> handler) {
        final Action<D> action = this.getAction(id);
        action.withHandler(condition, handler);
    }

    public <D, E> void registerHandler(@Nonnull Action.Id<D> id, @Nonnull BiConsumer<D, E> handler) {
        final Action<D> action = this.getAction(id);
        action.withHandler((r, e) -> true, handler);
    }

    @Nonnull
    public Shortcuts getIDEDefaultShortcuts() {
        return new Shortcuts() {
        };
    }

    public interface Shortcuts {
        default Object add() {
            return null;
        }

        default Object delete() {
            return null;
        }

        default Object view() {
            return null;
        }

        default Object edit() {
            return null;
        }

        default Object refresh() {
            return null;
        }

        default Object start() {
            return null;
        }

        default Object stop() {
            return null;
        }

        default Object restart() {
            return null;
        }

        default Object deploy() {
            return null;
        }

        default Object copy() {
            return null;
        }

        default Object paste() {
            return null;
        }
    }

    public static class DummyActionManager extends AzureActionManager {
        @Override
        public <D> void registerAction(Action<D> action) {
        }

        @Override
        public <D> Action<D> getAction(Action.Id<D> id) {
            if (id == Action.OPEN_URL) { // only open url is supported
                //noinspection unchecked
                return (Action<D>) new DummyOpenUrlAction();
            }
            return null;
        }

        @Override
        public void registerGroup(String id, ActionGroup group) {

        }

        @Override
        public ActionGroup getGroup(String id) {
            return null;
        }
    }

    public static class DummyOpenUrlAction extends Action<String> {
        private DummyOpenUrlAction() {
            super(Action.OPEN_URL);
        }

        public String toString(IAzureMessage.ValueDecorator decorator) {
            final IView.Label view = this.getView(this.target);
            return String.format("[%s](%s)", view.getLabel(), decorator.decorateValue(this.target, null));
        }
    }
}
