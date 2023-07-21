/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.view.IView;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface IActionGroup {

    IView.Label getView();

    List<Object> getActions();

    void addAction(Object action);

    default void appendAction(Object action) {
        this.addAction(action);
    }

    default void appendActions(@Nonnull Object... actions) {
        for (final Object action : actions) {
            this.appendAction(action);
        }
    }

    default void appendActions(@Nonnull Collection<Object> actions) {
        for (final Object action : actions) {
            this.appendAction(action);
        }
    }

    void prependAction(Object action);

    default void prependActions(@Nonnull Object... actions) {
        for (int i = actions.length - 1; i >= 0; i--) {
            this.prependAction(actions[i]);
        }
    }

    default void prependActions(@Nonnull Collection<Object> actions) {
        final ArrayList<Object> list = new ArrayList<>(actions);
        for (int i = list.size() - 1; i >= 0; i--) {
            this.prependAction(list.get(i));
        }
    }
}
