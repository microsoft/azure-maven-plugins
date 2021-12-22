/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.maven.common.action;

import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

public class MavenActionManager extends AzureActionManager {

    public static void register() {
        final MavenActionManager am = new MavenActionManager();
        register(am);
    }

    @Override
    public <D> void registerAction(Action.Id<D> id, Action<D> action) {
    }

    @Override
    public <D> Action<D> getAction(Action.Id<D> id) {
        return null;
    }

    @Override
    public void registerGroup(String id, ActionGroup group) {

    }

    @Override
    public ActionGroup getGroup(String id) {
        return null;
    }
}
