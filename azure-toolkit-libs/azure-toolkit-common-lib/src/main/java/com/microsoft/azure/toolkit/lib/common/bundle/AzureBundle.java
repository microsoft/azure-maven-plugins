/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.bundle;

import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class AzureBundle {
    private static final Map<String, Optional<ResourceBundle>> bundles = new ConcurrentHashMap<>();
    private static final String INDEX = "index";
    private final String pkg;

    @Nonnull
    public String message(@Nonnull final String key, final Object... params) {
        return message(this.pkg, key, params);
    }

    @Nullable
    public String pattern(@Nonnull final String key) {
        return pattern(this.pkg, key);
    }

    @Nonnull
    public static String message(@Nonnull final String pkg, @Nonnull final String key, final Object... params) {
        final String pattern = pattern(pkg, key);
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
    @Cacheable(cacheName = "bundle/package/{}/pattern/{}", key = "$pkg/$key")
    public static String pattern(@Nonnull final String pkg, @Nonnull final String key) {
        final List<ResourceBundle> bundles = getBundles(pkg, key);
        for (ResourceBundle bundle : bundles) {
            final String pattern = getPattern(key, bundle);
            if (Objects.nonNull(pattern)) {
                return pattern;
            }
        }
        return null;
    }

    private static List<ResourceBundle> getBundles(@Nonnull final String pkg, @Nonnull final String key) {
        final String supClass = key.split("[|.]")[0].toLowerCase();
        final String subClass = key.split("\\.")[0].replaceAll("\\|", "_").toLowerCase();
        final String exSub = String.format("%s.%s", pkg, subClass);
        final String exSup = String.format("%s.%s", pkg, supClass);
        final String exIdx = String.format("%s.%s", pkg, INDEX);
        final String sub = String.format("%s.base.%s", pkg, subClass);
        final String sup = String.format("%s.base.%s", pkg, supClass);
        final String idx = String.format("%s.base.%s", pkg, INDEX);
        return Stream.of(exSub, exSup, exIdx, sub, sup, idx)
                .map(fqn -> bundles.computeIfAbsent(fqn, k -> Optional.ofNullable(getBundle(fqn))))
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    @Nullable
    private static String getPattern(@Nonnull String key, @Nullable ResourceBundle bundle) {
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
    private static ResourceBundle getBundle(String bundleName) {
        try {
            return ResourceBundle.getBundle(bundleName);
        } catch (final Exception e) {
            return null;
        }
    }
}
