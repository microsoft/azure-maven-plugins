/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class AzureMessage implements IAzureMessage{
    @Nonnull
    public static AzureMessage.Context getContext() {
        return getContext(IAzureOperation.current());
    }

    @Nonnull
    public static AzureMessage.Context getContext(@Nullable IAzureOperation operation) {
        return Optional.ofNullable(operation)
                .map(o -> o.get(AzureMessage.Context.class, new AzureMessage.Context(operation)))
                .orElse(new AzureMessage.Context(operation));
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
                    .map(AzureMessage::getContext)
                    .map(Context::getMessager)
                    .orElse(AzureMessager.getDefaultMessager());
        }
    }
}
