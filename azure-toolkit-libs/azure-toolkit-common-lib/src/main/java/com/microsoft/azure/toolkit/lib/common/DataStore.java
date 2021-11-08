/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common;

import org.apache.commons.lang3.StringUtils;

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
    @Deprecated
    default <D> D get(Class<D> type, @Nonnull D dft) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            return (D) thisStore.computeIfAbsent(type, (t) -> dft);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @Deprecated
    default <D> D get(Class<D> type) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            return (D) thisStore.get(type);
        }
    }

    @Deprecated
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

    @Nonnull
    @SuppressWarnings("unchecked")
    default <D> D get(Field<D> key, @Nonnull D dft) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            return (D) thisStore.computeIfAbsent(key, (t) -> dft);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    default <D> D get(Field<D> key) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            return (D) thisStore.get(key);
        }
    }

    default <D> void set(Field<D> key, D val) {
        synchronized (Impl.store) {
            final Map<Object, Object> thisStore = Impl.store.computeIfAbsent(this, (k) -> new HashMap<>());
            thisStore.put(key, val);
        }
    }

    default void clearAll() {
        synchronized (Impl.store) {
            Impl.store.remove(this);
        }
    }

    final class Impl {
        static final WeakHashMap<Object, Map<Object, Object>> store = new WeakHashMap<>();
    }

    final class Field<T> {
        @Nonnull
        private final String name;

        private Field(@Nonnull String name) {
            this.name = name;
        }

        public static <D> Field<D> of(@Nonnull String name) {
            assert StringUtils.isNotBlank(name) : "field name can not be blank";
            return new Field<>(name);
        }

        @Nonnull
        public String getId() {
            return name;
        }
    }
}
