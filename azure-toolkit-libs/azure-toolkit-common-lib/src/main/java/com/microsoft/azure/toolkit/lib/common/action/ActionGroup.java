/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

@Getter
@Accessors(chain = true, fluent = true)
public class ActionGroup {
    @Nullable
    private IView.Label view;
    private final List<Object> actions;

    public ActionGroup(@Nonnull List<Object> actions) {
        this.actions = actions;
    }

    public ActionGroup(@Nonnull Object... actions) {
        this.actions = Arrays.asList(actions);
    }

    public ActionGroup(@Nonnull List<Object> actions, @Nullable IView.Label view) {
        this.view = view;
        this.actions = actions;
    }

    @Getter
    @Accessors(chain = true, fluent = true)
    public static class Proxy extends ActionGroup {
        @Nullable
        private final String id;
        @Nonnull
        private final ActionGroup group;

        public Proxy(@Nonnull ActionGroup group) {
            super(group.actions, group.view);
            this.id = null;
            this.group = group;
        }

        public Proxy(@Nonnull ActionGroup group, @Nonnull String id) {
            super(group.actions, group.view);
            this.id = id;
            this.group = group;
        }
    }
}
