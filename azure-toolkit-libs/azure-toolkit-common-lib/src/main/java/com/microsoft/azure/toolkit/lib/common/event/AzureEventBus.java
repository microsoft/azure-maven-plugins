/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NonNls;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class AzureEventBus {
    @NonNls
    private static final Map<String, EventBus> buses = new ConcurrentHashMap<>();

    public static void on(@Nonnull final String type, @Nonnull EventListener listener) {
        getBus(type).register(listener);
    }

    public static void off(@Nonnull final String type, @Nonnull EventListener listener) {
        try {
            getBus(type).unregister(listener);
        } catch (final IllegalArgumentException e) {
            // swallow exception during unregistering a listener which is not registered
        }
    }

    public static void once(@Nonnull final String type, @Nonnull BiConsumer<Object, Object> listener) {
        final EventBus bus = getBus(type);
        final EventListener[] listeners = new EventListener[1];
        listeners[0] = new EventListener((e) -> {
            listener.accept(e.getSource(), e.getPayload());
            bus.unregister(listeners[0]);
        });
        bus.register(listeners[0]);
    }

    public static void emit(@Nonnull final String type) {
        AzureEventBus.emit(type, new SimpleEvent(type, null));
    }

    public static void emit(@Nonnull final String type, @Nullable final Object source) {
        AzureEventBus.emit(type, new SimpleEvent(type, source));
    }

    public static void emit(@Nonnull final String type, @Nullable final Object source, @Nullable final Object payload) {
        AzureEventBus.emit(type, new SimpleEvent(type, source, payload));
    }

    public static <T> void emit(@Nonnull final String type, @Nonnull AzureEvent event) {
        getBus(type).post(event);
    }

    private static EventBus getBus(String eventType) {
        return buses.computeIfAbsent(eventType, (e) -> new AsyncEventBus(command -> Schedulers.boundedElastic().schedule(command)));
    }

    @RequiredArgsConstructor
    public static class EventListener {

        @Nonnull
        private final Consumer<AzureEvent> listener;

        @Subscribe
        public void onEvent(@Nonnull AzureEvent event) {
            this.listener.accept(event);
        }
    }

    @Getter
    @RequiredArgsConstructor
    @AllArgsConstructor
    private static class SimpleEvent implements AzureEvent {
        @Nonnull
        private final String type;
        @Nullable
        private final Object source;
        @Nullable
        private Object payload;
    }
}
