/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.view;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IView {

    void dispose();

    void refresh();

    interface Label extends IView {
        String getLabel();

        String getIconPath();

        String getDescription();

        default boolean isEnabled() {
            return true;
        }

        default void refresh() {
        }

        @Getter
        @RequiredArgsConstructor
        @AllArgsConstructor
        class Static implements Label {
            @Nonnull
            protected final String label;
            @Nullable
            protected String iconPath;
            @Nullable
            protected String description;

            public Static(String title, String iconPath) {
                this(title, iconPath, null);
            }

            @Override
            public void dispose() {
            }
        }
    }
}
