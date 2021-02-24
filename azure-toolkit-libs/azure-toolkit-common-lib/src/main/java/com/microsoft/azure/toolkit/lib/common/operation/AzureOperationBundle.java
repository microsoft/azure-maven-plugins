/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.Objects;

public class AzureOperationBundle {

    @NonNls
    public static final String TITLES = "com.microsoft.azure.toolkit.operation.titles";
    private static Provider provider;

    public static synchronized void register(final Provider provider) {
        if (AzureOperationBundle.provider == null) {
            AzureOperationBundle.provider = provider;
        }
    }

    public static IAzureOperationTitle title(@NotNull @PropertyKey(resourceBundle = TITLES) String name, @NotNull Object... params) {
        return MessageBundleBasedOperationTitle.builder().name(name).params(params).build();
    }

    @Builder
    @Getter
    public static class MessageBundleBasedOperationTitle implements IAzureOperationTitle {
        private final String name;
        private final Object[] params;
        private String title;

        public String toString() {
            if (Objects.isNull(this.title)) {
                this.title = provider.getMessage(this.name, params);
            }
            return this.title;
        }
    }

    public interface Provider {
        String getMessage(@NotNull String key, @NotNull Object... params);
    }
}
