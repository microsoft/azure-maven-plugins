/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.DataStore;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;

import javax.annotation.Nullable;

public interface IAzureOperation extends DataStore<IAzureOperation.IContext> {
    String UNKNOWN_NAME = "<unknown>.<unknown>";

    String getId();

    String getName();

    String getType();

    Object getTitle();

    void setParent(IAzureOperation operation);

    @Nullable
    IAzureOperation getParent();

    default IAzureOperation getEffectiveParent() {
        final IAzureOperation parent = this.getParent();
        if (parent == null) {
            return null;
        } else if (!parent.getName().equals(UNKNOWN_NAME)) {
            return parent;
        } else {
            return parent.getEffectiveParent();
        }
    }

    default IAzureOperation getActionParent() {
        final IAzureOperation parent = this.getParent();
        if (parent == null) {
            return null;
        } else if (parent.getType().equals(AzureOperation.Type.ACTION.name())) {
            return parent;
        } else {
            return parent.getActionParent();
        }
    }

    @Nullable
    static IAzureOperation current() {
        return AzureTaskContext.current().currentOperation();
    }

    interface IContext {

    }
}
