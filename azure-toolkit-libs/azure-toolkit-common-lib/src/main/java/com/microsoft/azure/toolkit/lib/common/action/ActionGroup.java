/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Getter
public class ActionGroup implements IActionGroup {
    @Nullable
    private IView.Label view;
    private final List<Object> actions = new ArrayList<>();
    @Setter
    private Object origin; // ide's action group.

    public ActionGroup(@Nonnull List<Object> actions) {
        this.actions.addAll(actions);
    }

    public ActionGroup(Object... actions) {
        if (actions != null) {
            this.actions.addAll(Arrays.asList(actions));
        }
    }

    public ActionGroup(@Nonnull List<Object> actions, @Nullable IView.Label view) {
        this.view = view;
        this.actions.addAll(actions);
    }

    @Override
    public void addAction(Object action) {
        this.actions.add(action);
    }

    @Override
    public void appendActions(@Nonnull Object... actions) {
        this.actions.addAll(Arrays.asList(actions));
    }

    @Override
    public void appendActions(@Nonnull Collection<Object> actions) {
        this.actions.addAll(actions);
    }

    @Override
    public void prependAction(Object action) {
        this.actions.add(0, action);
    }

    @Override
    public void prependActions(@Nonnull Object... actions) {
        this.actions.addAll(0, Arrays.asList(actions));
    }

    @Override
    public void prependActions(@Nonnull Collection<Object> actions) {
        this.actions.addAll(0, actions);
    }
}
