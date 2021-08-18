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
public interface DataStore {
    @Nonnull
    @SuppressWarnings("unchecked")
    default <D> D get(Class<D> type, @Nonnull D dft) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            return (D) thisStore.computeIfAbsent(type, (t) -> dft);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    default <D> D get(Class<D> type) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            return (D) thisStore.get(type);
        }
    }

    default <D> void set(Class<D> type, D val) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            thisStore.put(type, val);
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    default <D> D get(String key, @Nonnull D dft) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            return (D) thisStore.computeIfAbsent(key, (t) -> dft);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    default <D> D get(String key) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            return (D) thisStore.get(key);
        }
    }

    default <D> void set(String key, D val) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            thisStore.put(key, val);
        }
    }

    final class Impl {
        static final WeakHashMap<Object, Map<Object, Object>> store = new WeakHashMap<>();
    }
}