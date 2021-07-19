/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureBundle;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.bundle.CustomDecoratable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

public interface IAzureMessage {
    @Nonnull
    AzureString getMessage();

    @Nonnull
    default String getContent() {
        return this.getMessage().getString();
    }

    @Nonnull
    Type getType();

    @Nullable
    String getTitle();

    @Nullable
    Object getPayload();

    @Nullable
    Action[] getActions();

    default boolean show() {
        return AzureMessager.getMessager().show(this);
    }

    default boolean show(IAzureMessager messager) {
        return messager.show(this);
    }

    /**
     * @return null if not decoratable
     */
    @Nullable
    default String decorateValue(@Nonnull Object p, @Nullable Supplier<String> dft) {
        String result = null;
        if (p instanceof CustomDecoratable) {
            result = ((CustomDecoratable) p).decorate(this);
        }
        return Objects.isNull(result) && Objects.nonNull(dft) ? dft.get() : result;
    }

    /**
     * @return null if not decoratable
     */
    @Nullable
    default String decorateText(@Nonnull AzureString text, @Nullable Supplier<String> dft) {
        String result = null;
        if (text instanceof CustomDecoratable) {
            result = ((CustomDecoratable) text).decorate(this);
        }
        if (Objects.isNull(result)) {
            final Object[] params = Arrays.stream(text.getParams())
                    .map((p) -> this.decorateValue(p, p::toString))
                    .toArray();
            final AzureBundle bundle = text.getBundle();
            if (Objects.nonNull(bundle)) {
                result = bundle.message(text.getName(), params);
            } else {
                try {
                    result = MessageFormat.format(text.getName(), params);
                } catch (final IllegalArgumentException e) {
                    result = null;
                }
            }
        }
        return Objects.isNull(result) && Objects.nonNull(dft) ? dft.get() : result;
    }

    enum Type {
        INFO, WARNING, SUCCESS, ERROR, ALERT, CONFIRM
    }

    interface ValueDecorator {
        String decorateValue(@Nonnull Object p, @Nullable IAzureMessage message);
    }

    interface Action {
        String name();

        void actionPerformed(IAzureMessage message);
    }
}
