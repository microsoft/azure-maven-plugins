/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class AzureMessageBundle {
    private static final Map<String, Optional<ResourceBundle>> libBundles = new ConcurrentHashMap<>();
    private static final Map<String, Optional<ResourceBundle>> toolBundles = new ConcurrentHashMap<>();
    private static final String ALL = "<ALL>";
    private final String base;
    private final String toolPostfix;

    @Nonnull
    public String getMessage(@Nonnull final String key, final Object... params) {
        final String pattern = this.getPattern(key);
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
    public String getPattern(@Nonnull final String key) {
        final String subGroup = key.split("\\.")[0].replaceAll("\\|", "_");
        final String supGroup = key.split("[|.]")[0];
        final ArrayList<Supplier<String>> suppliers = new ArrayList<>();
        suppliers.add(() -> this.getToolMessagePattern(subGroup, key));
        suppliers.add(() -> this.getToolMessagePattern(supGroup, key));
        suppliers.add(() -> this.getToolMessagePattern(ALL, key));
        suppliers.add(() -> this.getLibMessagePattern(subGroup, key));
        suppliers.add(() -> this.getLibMessagePattern(supGroup, key));
        suppliers.add(() -> this.getLibMessagePattern(ALL, key));
        for (final Supplier<String> supplier : suppliers) {
            final String pattern = supplier.get();
            if (Objects.nonNull(pattern)) {
                return pattern;
            }
        }
        return null;
    }

    public String getLibMessagePattern(@Nonnull final String group, @Nonnull final String key) {
        return libBundles.computeIfAbsent(group, k -> {
            final String bundleName = ALL.equals(group) ?
                    this.base :
                    String.format("%s_%s", this.base, group);
            return Optional.ofNullable(getBundle(bundleName));
        }).map(b -> getPattern(key, b)).orElse(null);
    }

    public String getToolMessagePattern(@Nonnull final String group, @Nonnull final String key) {
        return toolBundles.computeIfAbsent(group, k -> {
            final String bundleName = ALL.equals(group) ?
                    String.format("%s_%s", this.base, this.toolPostfix) :
                    String.format("%s_%s_%s", this.base, group, this.toolPostfix);
            return Optional.ofNullable(getBundle(bundleName));
        }).map(b -> getPattern(key, b)).orElse(null);
    }

    @Nullable
    private String getPattern(@Nonnull String key, @Nullable ResourceBundle bundle) {
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
    private ResourceBundle getBundle(String bundleName) {
        try {
            return ResourceBundle.getBundle(bundleName);
        } catch (final Exception e) {
            return null;
        }
    }
}
