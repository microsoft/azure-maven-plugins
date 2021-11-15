/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.bundle;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@RequiredArgsConstructor
public class AzureBundle {
    private final ResourceBundle bundle;

    public AzureBundle(@Nonnull String bundleName) {
        this.bundle = getBundle(bundleName, this.getClass());
    }

    public AzureBundle(@Nonnull String bundleName, Class<?> clazz) {
        this.bundle = getBundle(bundleName, clazz);
    }

    @Nonnull
    public String getMessage(@Nonnull final String key, final Object... params) {
        final String pattern = getPattern(key);
        if (StringUtils.isBlank(pattern)) {
            return String.format("!%s!", key);
        }
        try {
            return MessageFormat.format(pattern, params);
        } catch (final IllegalArgumentException e) {
            return pattern;
        }
    }

    @Nullable
    public String getPattern(@Nonnull String key) {
        if (StringUtils.isBlank(key) || Objects.isNull(bundle)) {
            return null;
        }
        try {
            return bundle.getString(key);
        } catch (final MissingResourceException e) {
            return null;
        }
    }

    @Nullable
    private static ResourceBundle getBundle(String bundleName, Class<?> clazz) {
        try {
            return ResourceBundle.getBundle(bundleName, Locale.getDefault(), clazz.getClassLoader());
        } catch (final Exception e) {
            return null;
        }
    }
}
