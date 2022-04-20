/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AzureActionManager {

    @Getter
    private static AzureActionManager instance;

    protected static void register(AzureActionManager manager) {
        if (instance != null) {
            AzureMessager.getDefaultMessager().warning("ActionManager is already registered", null);
            return;
        }
        instance = manager;
    }

    public abstract <D> void registerAction(Action.Id<D> id, Action<D> action);

    public <D> void registerAction(Action.Id<D> id, Consumer<D> action) {
        this.registerAction(id, new Action<>(action));
    }

    public abstract <D> Action<D> getAction(Action.Id<D> id);

    public abstract void registerGroup(String id, ActionGroup group);

    public abstract IActionGroup getGroup(String id);

    public <D> void registerHandler(@Nonnull Action.Id<D> id, @Nonnull Predicate<D> condition, @Nonnull Consumer<D> handler) {
        final Action<D> action = this.getAction(id);
        action.registerHandler(condition, handler);
    }

    public <D> void registerHandler(@Nonnull Action.Id<D> id, @Nonnull Consumer<D> handler) {
        final Action<D> action = this.getAction(id);
        action.registerHandler(o -> true, handler);
    }

    public <D, E> void registerHandler(@Nonnull Action.Id<D> id, @Nonnull BiPredicate<D, E> condition, @Nonnull BiConsumer<D, E> handler) {
        final Action<D> action = this.getAction(id);
        action.registerHandler(condition, handler);
    }

    public <D, E> void registerHandler(@Nonnull Action.Id<D> id, @Nonnull BiConsumer<D, E> handler) {
        final Action<D> action = this.getAction(id);
        action.registerHandler((r, e) -> true, handler);
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
    }
}
