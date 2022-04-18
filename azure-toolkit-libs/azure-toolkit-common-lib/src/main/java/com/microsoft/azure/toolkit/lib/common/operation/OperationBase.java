/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.utils.Utils;

import javax.annotation.Nonnull;
import java.util.Objects;

public abstract class OperationBase<T> implements Operation<T> {

    private OperationContext context;

    @Nonnull
    public String getExecutionId() {
        return Utils.getId(this);
    }

    @Override
    public synchronized OperationContext getContext() {
        if (Objects.isNull(this.context)) {
            this.context = new OperationContext(this);
        }
        return this.context;
    }
}
