/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.DataStore;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public interface IAzureOperation extends DataStore {
    String UNKNOWN_NAME = "<unknown>.<unknown>";

    @Nonnull
    default String getId() {
        return Utils.getId(this);
    }

    @Nonnull
    default String getName() {
        return Optional.ofNullable(this.getTitle()).map(AzureString::getName).orElse(UNKNOWN_NAME);
    }

    @Nonnull
    String getType();

    @Nullable
    AzureString getTitle();

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

    @Nullable
    default IAzureOperation getActionParent() {
        if (this.getType().equals(AzureOperation.Type.ACTION.name())) {
            return this;
        }
        return Optional.ofNullable(this.getParent()).map(IAzureOperation::getActionParent).orElse(null);
    }

    @Nullable
    static IAzureOperation current() {
        return AzureTaskContext.current().currentOperation();
    }

    interface IContext {

    }
}
