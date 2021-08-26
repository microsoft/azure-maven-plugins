/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class AzureEventBus {
    @NonNls
    private static final Map<String, EventBus> buses = new ConcurrentHashMap<>();

    public static <T, E extends AzureEvent<T>> void on(@Nonnull final String type, @Nonnull EventListener<T, E> listener) {
        getBus(type).register(listener);
    }

    public static <T, E extends AzureEvent<T>> void off(@Nonnull final String type, @Nonnull EventListener<T, E> listener) {
        getBus(type).unregister(listener);
    }

    public static <T, E extends AzureEvent<T>> void on(@Nonnull final String type, @Nonnull Consumer<T> listener) {
        getBus(type).register(new EventListener<T, E>((e) -> listener.accept(e.getPayload())));
    }

    public static <T, E extends AzureEvent<T>> void after(@Nonnull final String operation, @Nonnull Consumer<T> listener) {
        getBus(operation).register(new EventListener<T, E>((e) -> {
            if (e instanceof AzureOperationEvent && ((AzureOperationEvent<?>) e).getStage() == AzureOperationEvent.Stage.AFTER) {
                listener.accept(e.getPayload());
            }
        }));
    }

    public static <T, E extends AzureEvent<T>> void before(@Nonnull final String operation, @Nonnull Consumer<T> listener) {
        getBus(operation).register(new EventListener<T, E>((e) -> {
            if (e instanceof AzureOperationEvent && ((AzureOperationEvent<?>) e).getStage() == AzureOperationEvent.Stage.BEFORE) {
                listener.accept(e.getPayload());
            }
        }));
    }

    public static <T, E extends AzureEvent<T>> void error(@Nonnull final String operation, @Nonnull Consumer<T> listener) {
        getBus(operation).register(new EventListener<T, E>((e) -> {
            if (e instanceof AzureOperationEvent && ((AzureOperationEvent<?>) e).getStage() == AzureOperationEvent.Stage.ERROR) {
                listener.accept(e.getPayload());
            }
        }));
    }

    public static void emit(@Nonnull final String type) {
        AzureEventBus.emit(type, new SimpleEvent<>(type, null));
    }

    public static void emit(@Nonnull final String type, @Nullable final Object source) {
        AzureEventBus.emit(type, new SimpleEvent<>(type, source));
    }

    public static <T> void emit(@Nonnull final String type, @Nonnull AzureEvent<T> event) {
        getBus(type).post(event);
    }

    private static EventBus getBus(String eventType) {
        return buses.computeIfAbsent(eventType, EventBus::new);
    }

    @RequiredArgsConstructor
    public static class EventListener<T, E extends AzureEvent<T>> {

        @Nonnull
        private final Consumer<E> listener;

        @Subscribe
        public void onEvent(@Nonnull E event) {
            this.listener.accept(event);
        }
    }

    @Getter
    @RequiredArgsConstructor
    @AllArgsConstructor
    private static class SimpleEvent<T> implements AzureEvent<T> {
        @Nonnull
        private final String type;
        @Nullable
        private final Object source;
        @Nullable
        private T payload;
    }
}
