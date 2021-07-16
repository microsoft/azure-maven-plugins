/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.bundle;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public class AzureText {

    @Nullable
    private final AzureBundle bundle;
    @Nonnull
    private final String name;
    private final Object[] params;

    @Nonnull
    public static AzureText fromText(@Nonnull String text) {
        return fromPattern(text);
    }

    @Nonnull
    public static AzureText fromPattern(@Nonnull String pattern, Object... params) {
        return new AzureText(null, pattern, params);
    }

    @Nonnull
    public static AzureText fromBundle(@Nonnull AzureBundle bundle, @Nonnull String key, Object... params) {
        return new AzureText(bundle, key, params);
    }

    public String getText() {
        final String pattern = Objects.nonNull(bundle) ? bundle.pattern(name) : name;
        try {
            if (StringUtils.isBlank(pattern)) {
                return String.format("!%s!", name);
            }
            return MessageFormat.format(pattern, params);
        } catch (final IllegalArgumentException e) {
            return pattern;
        }
    }

    public String toString() {
        return this.getText();
    }
}
