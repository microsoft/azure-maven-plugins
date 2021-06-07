/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class AzureMessager {
    private static IAzureMessager defaultMessager;

    public static synchronized void setDefaultMessager(@Nonnull IAzureMessager messager) {
        if (AzureMessager.defaultMessager == null) { // not allow overwriting...
            AzureMessager.defaultMessager = messager;
        } else {
            AzureMessager.getMessager().warning("default messager has already been registered");
        }
    }

    @Nonnull
    public static IAzureMessager getDefaultMessager() {
        return Optional.ofNullable(AzureMessager.defaultMessager)
                .orElse(new DummyMessager());
    }

    @Nonnull
    public static IAzureMessager getMessager() {
        return getContext().getMessager();
    }

    @Nonnull
    public static AzureMessager.Context getContext() {
        return getContext(IAzureOperation.current());
    }

    @Nonnull
    public static AzureMessager.Context getContext(@Nullable IAzureOperation operation) {
        return Optional.ofNullable(operation)
                .map(o -> o.get(AzureMessager.Context.class, new AzureMessager.Context(operation)))
                .orElse(new AzureMessager.Context(operation));
    }

    @RequiredArgsConstructor
    public static class Context implements IAzureOperation.IContext {
        @Nullable
        private final IAzureOperation operation;
        private IAzureMessager messager = null;
        private final Map<String, Object> properties = new HashMap<>();

        public void set(@Nonnull String key, Object val) {
            this.properties.put(key, val);
        }

        public Object get(@Nonnull String key) {
            return this.properties.get(key);
        }

        public void setMessager(@Nonnull IAzureMessager messager) {
            this.messager = messager;
        }

        @Nonnull
        public IAzureMessager getMessager() {
            if (Objects.nonNull(this.messager)) {
                return messager;
            }
            return getMessager(Optional.ofNullable(this.operation).map(IAzureOperation::getParent).orElse(null));
        }

        @Nonnull
        public IAzureMessager getActionMessager() {
            return getMessager(Optional.ofNullable(this.operation).map(IAzureOperation::getActionParent).orElse(null));
        }

        @Nonnull
        private IAzureMessager getMessager(@Nullable IAzureOperation op) {
            return Optional.ofNullable(op)
                    .map(AzureMessager::getContext)
                    .map(Context::getMessager)
                    .orElse(AzureMessager.getDefaultMessager());
        }
    }

    @Slf4j
    public static class DummyMessager implements IAzureMessager {
        @Override
        public boolean show(IAzureMessage message) {
            log.info("DUMMY MESSAGE:{}", message);
            return false;
        }
    }
}
