/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model.page;

import com.azure.core.http.rest.Page;
import com.azure.core.util.IterableStream;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.stream.Stream;

public class ItemPage<T> implements Page<T> {
    private final IterableStream<T> items;

    public ItemPage(@Nonnull final Iterable<T> items) {
        this.items = IterableStream.of(items);
    }

    public ItemPage(@Nonnull final Stream<T> items) {
        this.items = new IterableStream<T>(Flux.fromStream(items));
    }

    public static <T> ItemPage<T> emptyPage() {
        return new ItemPage<>(Collections.emptyList());
    }

    @Override
    public IterableStream<T> getElements() {
        return items;
    }

    @Override
    public String getContinuationToken() {
        return null;
    }
}
