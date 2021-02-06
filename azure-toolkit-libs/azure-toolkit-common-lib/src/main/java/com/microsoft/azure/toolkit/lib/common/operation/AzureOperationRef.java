/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.Objects;

@Getter
@Builder
public class AzureOperationRef implements IAzureOperation {
    private final Method method;
    private final String[] paramNames;
    private final Object[] paramValues;
    private final Object instance;

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AzureOperationRef)) {
            return false;
        }
        final AzureOperationRef operation = (AzureOperationRef) obj;
        return Objects.equals(operation.getMethod(), this.getMethod());
    }

    @Override
    public String toString() {
        final AzureOperation annotation = AzureOperationUtils.getAnnotation(this);
        return String.format("{title:'%s', method:%s}", annotation.name(), method.getName());
    }

    public String getName() {
        final AzureOperation annotation = AzureOperationUtils.getAnnotation(this);
        return annotation.name();
    }

    public String getType() {
        final AzureOperation annotation = AzureOperationUtils.getAnnotation(this);
        return annotation.type().name();
    }

    public String getId() {
        return Utils.getId(this);
    }
}
