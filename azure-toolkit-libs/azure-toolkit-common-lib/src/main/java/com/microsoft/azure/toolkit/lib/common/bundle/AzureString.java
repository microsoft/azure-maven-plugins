/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.bundle;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public class AzureString {

    @Nullable
    private final AzureBundle bundle;
    @Nonnull
    private final String name;
    @Nullable
    private final Object[] params;

    @Nonnull
    public static AzureString fromString(@Nonnull String text) {
        return format(text);
    }

    @Nonnull
    public static AzureString format(@Nonnull String pattern, Object... params) {
        return new AzureString(null, pattern, params);
    }

    @Nonnull
    public static AzureString format(@Nonnull AzureBundle bundle, @Nonnull String key, Object... params) {
        return new AzureString(bundle, key, ObjectUtils.firstNonNull(params, new Object[0]));
    }

    public String getString() {
        return this.getString(this.params);
    }

    public String getString(Object... params) {
        if (StringUtils.isBlank(this.name) || (Objects.isNull(bundle) && ArrayUtils.isEmpty(params))) { // no need to resolve.
            return this.name;
        }
        final String pattern = Objects.nonNull(bundle) ? bundle.getPattern(name) : name;
        try {
            if (StringUtils.isBlank(pattern)) {
                return String.format("!%s!", name);
            }
            if (pattern.contains("{0}")) {
                return MessageFormat.format(pattern, params);
            }
            return String.format(pattern, params);
        } catch (final IllegalArgumentException e) {
            return pattern;
        }
    }

    public String toString() {
        return this.getString();
    }
}
