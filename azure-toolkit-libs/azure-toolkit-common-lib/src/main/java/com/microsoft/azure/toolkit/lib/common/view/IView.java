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
import java.util.Optional;

public interface IView {

    void dispose();

    interface Dynamic extends IView {

        default void updateView() {
            Optional.ofNullable(this.getUpdater()).ifPresent(Updater::updateView);
        }

        default void updateChildren() {
            Optional.ofNullable(this.getUpdater()).ifPresent(Updater::updateChildren);
        }

        void setUpdater(Updater updater);

        @Nullable
        Updater getUpdater();

        interface Updater {
            default void updateView() {
            }

            default void updateChildren() {
            }
        }
    }

    interface Label extends IView {
        String getLabel();

        String getIconPath();

        String getDescription();

        default boolean isEnabled() {
            return true;
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
