package com.microsoft.azure.toolkit.lib.common.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AzureEventBus {
    private static Map<String, EventBus> buses = new ConcurrentHashMap<>();

    public static <T, E extends AzureEvent<T>> void on(@Nonnull final String type, @Nonnull EventListener<T, E> listener) {
        getBus(type).register(listener);
    }

    public static <T, E extends AzureEvent<T>> void off(@Nonnull final String type, @Nonnull EventListener<T, E> listener) {
        getBus(type).unregister(listener);
    }

    public static <T> void on(@Nonnull final String type, @Nonnull Consumer<T> listener) {
        getBus(type).register(new EventListener<T, AzureEvent.Simple<T>>((e) -> listener.accept(e.getPayload())));
    }

    public static void emit(@Nonnull final String type) {
        AzureEventBus.emit(type, AzureEvent.simple(type, null));
    }

    public static <T> void emit(@Nonnull final String type, @Nullable final T payload) {
        AzureEventBus.emit(type, AzureEvent.simple(type, payload));
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
}
