package com.microsoft.azure.toolkit.lib.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AzureEvent<T> {
    @Nullable
    T getPayload();

    static <T> AzureEvent<T> simple(@Nonnull final String type, @Nullable final T payload) {
        return new Simple<>(type, payload);
    }

    @Getter
    @RequiredArgsConstructor
    class Simple<T> implements AzureEvent<T> {
        @Nonnull
        private final String type;
        @Nullable
        private final T payload;
    }
}
