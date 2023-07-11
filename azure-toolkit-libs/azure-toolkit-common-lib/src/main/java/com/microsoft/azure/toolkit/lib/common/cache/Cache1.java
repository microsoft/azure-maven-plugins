/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("UnusedReturnValue")
public class Cache1<T> {
    private static final ThreadLocal<Stack<Cache1<?>>> caching = ThreadLocal.withInitial(Stack::new);
    private static final String KEY = "CACHE1_KEY";
    @Nonnull
    private final LoadingCache<String, Optional<T>> cache;
    @Nonnull
    private final Supplier<T> supplier;
    @Nonnull
    private final AtomicReference<String> status = new AtomicReference<>();
    private BiConsumer<T, T> onNewValue = (n, o) -> {
    };
    private Consumer<String> onNewStatus = s -> {
    };
    private T latest = null;

    public Cache1(@Nonnull Supplier<T> supplier) {
        this.supplier = supplier;
        this.cache = Caffeine.newBuilder().build(key -> Cache1.this.load());
    }

    public Cache1<T> onValueChanged(BiConsumer<T, T> onNewValue) {
        this.onNewValue = onNewValue;
        return this;
    }

    public Cache1<T> onStatusChanged(Consumer<String> onNewStatus) {
        this.onNewStatus = onNewStatus;
        return this;
    }

    @Nullable
    private Optional<T> load() {
        if (AzureTaskManager.getInstance().isUIThread()) {
            return Optional.ofNullable(this.latest);
        }
        final String originalStatus = Status.LOADING;
        try {
            caching.get().push(this);
            this.setStatus(originalStatus);
            final T oldValue = this.latest;
            final T newValue = this.latest = supplier.get();
            final Optional<T> result = Optional.ofNullable(newValue);
            if (this.compareAndSetStatus(originalStatus, Status.OK)) {
                AzureTaskManager.getInstance().runOnPooledThread(() -> this.onNewValue.accept(newValue, oldValue));
                return result;
            }
        } catch (final Throwable e) {
            final Throwable root = ExceptionUtils.getRootCause(e);
            if (!(root instanceof InterruptedException) && this.compareAndSetStatus(originalStatus, Status.UNKNOWN)) {
                throw e;
            }
        } finally {
            caching.get().pop();
        }
        this.compareAndSetStatus(originalStatus, null);
        // noinspection OptionalAssignedToNull,ReturnOfNull
        return null;// ignore loaded value
    }

    @Nullable
    private Optional<T> update(@Nonnull Callable<T> body, String status, T oldValue) {
        final String originalStatus = Optional.ofNullable(status).orElse(Status.UPDATING);
        try {
            caching.get().push(this);
            this.setStatus(originalStatus);
            final T value = this.latest = body.call();
            final Optional<T> result = Optional.ofNullable(value);
            final T newValue = result.orElse(null);
            if (this.compareAndSetStatus(originalStatus, Status.OK)) {
                if (!Objects.equals(newValue, oldValue)) {
                    AzureTaskManager.getInstance().runOnPooledThread(() -> this.onNewValue.accept(newValue, oldValue));
                }
                return result;
            }
        } catch (final Throwable e) {
            if (this.compareAndSetStatus(originalStatus, Status.UNKNOWN)) {
                throw (e instanceof AzureToolkitRuntimeException) ? (AzureToolkitRuntimeException) e : new AzureToolkitRuntimeException(e);
            }
        } finally {
            caching.get().pop();
        }
        this.compareAndSetStatus(originalStatus, null);
        // noinspection OptionalAssignedToNull,ReturnOfNull
        return null;
    }

    @Nullable
    @SneakyThrows
    public T update(@Nonnull Callable<T> body, String status) {
        if (AzureTaskManager.getInstance().isUIThread()) {
            log.debug("!!!!!!!!!!!!!!!!! Calling Cache1.update() in UI thread may block UI.");
            log.debug(Arrays.stream(Thread.currentThread().getStackTrace()).map(t -> "\tat " + t).collect(Collectors.joining("\n")));
            return this.latest;
        }
        if (caching.get().contains(this)) {
            return body.call();
        }
        final T oldValue = this.getIfPresent();
        this.cache.invalidate(KEY);
        try {
            return Optional.ofNullable(this.cache.get(KEY, (key) -> update(body, status, oldValue))).flatMap(o -> o).orElse(null);
        } catch (final Throwable e) {
            throw e.getCause();
        }
    }

    @Nullable
    public T update(@Nonnull Runnable body, String status) {
        return this.update(() -> {
            body.run();
            return supplier.get();
        }, status);
    }

    @Nullable
    public T getIfPresent() {
        return this.getIfPresent(false);
    }

    @Nullable
    @SuppressWarnings("OptionalAssignedToNull")
    public T getIfPresent(boolean loadIfAbsent) {
        if (caching.get().contains(this)) {
            return this.latest;
        }
        final Optional<T> opt = this.cache.getIfPresent(KEY);
        if (opt == null) {
            if (loadIfAbsent) {
                AzureTaskManager.getInstance().runOnPooledThread(this::refresh);
            }
            return this.latest;
        }
        return opt.orElse(null);
    }

    public void refresh() {
        if (this.status.compareAndSet(null, Status.LOADING) ||
            this.status.compareAndSet(Status.OK, Status.LOADING) ||
            this.status.compareAndSet(Status.UNKNOWN, Status.LOADING)) {
            this.cache.refresh(KEY);
        }
    }

    @Nullable
    public T get() {
        if (AzureTaskManager.getInstance().isUIThread()) {
            //todo: show error message in debug/test mode
            log.debug("!!!!!!!!!!!!!!!!! Calling Cache1.get() in UI thread may block UI.");
            log.debug(Arrays.stream(Thread.currentThread().getStackTrace()).map(t -> "\tat " + t).collect(Collectors.joining("\n")));
            return this.latest;
        }
        if (caching.get().contains(this)) {
            return this.latest;
        }
        try {
            final Optional<T> value = this.cache.get(KEY);
            // noinspection OptionalAssignedToNull
            if (value == null) {// value is dropped.
                return this.latest;
            }
            return value.orElse(null);
        } catch (final IllegalStateException e) {
            return this.latest;
        } catch (final CompletionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    public void invalidate() {
        if (caching.get().contains(this) || this.isProcessing()) {
            this.status.set(null); // drop loading value.
            return;
        }
        if (this.status.compareAndSet(Status.OK, null) || this.status.compareAndSet(Status.UNKNOWN, null)) {
            this.cache.invalidateAll();
        }
    }

    private boolean compareAndSetStatus(String o, String n) {
        if (this.status.compareAndSet(o, n)) {
            Optional.ofNullable(this.onNewStatus).ifPresent(c -> c.accept(this.status.get()));
            return true;
        }
        return false;
    }

    private void setStatus(String n) {
        this.status.set(n);
        Optional.ofNullable(this.onNewStatus).ifPresent(c -> c.accept(this.status.get()));
    }

    public String getStatus() {
        return this.status.get();
    }

    private boolean isProcessing() {
        return !StringUtils.equalsAnyIgnoreCase(this.getStatus(), Status.OK, Status.UNKNOWN);
    }

    public interface Status {
        String LOADING = "Loading";
        String UPDATING = "Updating";
        String OK = "OK";
        String UNKNOWN = "Unknown";
    }
}
