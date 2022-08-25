/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.cache;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public class LRUStack<T> {
    private final int size;

    public LRUStack() {
        this.size = 5;
    }

    private final Map<T, T> data = Collections.synchronizedMap(new LinkedHashMap<T, T>(5, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<T, T> eldest) {
            return this.size() > size;
        }
    });

    public T peek(Predicate<T> condition) {
        return Lists.reverse(new LinkedList<>(this.data.values())).stream().filter(condition).findFirst().orElse(null);
    }

    public T peek() {
        if (this.data.isEmpty()) {
            return null;
        }
        return new LinkedList<>(this.data.values()).getLast();
    }

    public void push(T v) {
        final T t = this.data.get(v);
        if (Objects.isNull(t)) {
            this.data.put(v, v);
        }
    }
}
