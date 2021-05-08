/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * better not override equals() and hashcode() if use default get/set
 */
public interface DataStore<T> {
    @Nonnull
    @SuppressWarnings("unchecked")
    default <D extends T> D get(Class<D> type, @Nonnull D dft) {
        final Map<Class<?>, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
        return (D) thisStore.computeIfAbsent(type, (t) -> dft);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    default <D extends T> D get(Class<D> type) {
        final Map<Class<?>, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
        return (D) thisStore.get(type);
    }

    default <D extends T> void set(Class<D> type, D val) {
        final Map<Class<?>, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
        thisStore.put(type, val);
    }
}

final class Impl {
    static WeakHashMap<Object, Map<Class<?>, Object>> store = new WeakHashMap<>();
}
