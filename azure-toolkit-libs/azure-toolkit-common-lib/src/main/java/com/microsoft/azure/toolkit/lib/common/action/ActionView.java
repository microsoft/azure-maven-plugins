/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class ActionView implements IView.Label {

    @Nonnull
    private final String label;
    private final String iconPath;
    @Nullable
    private AzureString title;
    private final boolean enabled;

    @Override
    public String getDescription() {
        return Optional.ofNullable(this.title).map(AzureString::toString).orElse(null);
    }

    @Override
    public void dispose() {
    }

    @RequiredArgsConstructor
    @Setter
    @Getter
    @Accessors(chain = true, fluent = true)
    public static class Builder {
        @Nonnull
        protected final Function<Object, String> label;
        @Nullable
        protected Function<Object, String> iconPath;
        @Nullable
        protected Function<Object, AzureString> title;
        @Nullable
        protected Function<Object, Boolean> enabled = s -> true;

        public Builder(String label) {
            this(s -> label);
        }

        public Builder(String label, String iconPath) {
            this(s -> label);
            this.iconPath = (s) -> iconPath;
        }

        public ActionView toActionView(Object s) {
            try {
                final Boolean e = Optional.ofNullable(this.enabled).map(p -> p.apply(s)).orElse(true);
                final String i = Optional.ofNullable(this.iconPath).map(p -> p.apply(s)).orElse(null);
                final AzureString t = Optional.ofNullable(this.title).map(p -> p.apply(s)).orElse(null);
                return new ActionView(this.label.apply(s), i, t, e);
            } catch (final Exception e) {
                return new ActionView("", "", false);
            }
        }
    }
}
