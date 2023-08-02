/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.utils.aspect.ExpressionUtils;
import com.microsoft.azure.toolkit.lib.common.utils.aspect.MethodInvocation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.concurrent.Callable;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class MethodOperation extends OperationBase {

    @EqualsAndHashCode.Include
    private final MethodInvocation invocation;

    @Override
    public String toString() {
        final AzureOperation annotation = this.invocation.getAnnotation(AzureOperation.class);
        final String name = StringUtils.firstNonBlank(annotation.name(), annotation.value());
        return String.format("{name:'%s', method:%s}", name, this.invocation.getMethod().getName());
    }

    @Nonnull
    public String getId() {
        final AzureOperation annotation = this.invocation.getAnnotation(AzureOperation.class);
        return StringUtils.firstNonBlank(annotation.name(), annotation.value());
    }

    @Override
    public Callable<Object> getBody() {
        return this.invocation::invoke;
    }

    @Nullable
    @Override
    public Object getSource() {
        final AzureOperation annotation = this.invocation.getAnnotation(AzureOperation.class);
        final String sourceExpression = annotation.source();
        if (StringUtils.isNotBlank(sourceExpression)) {
            return ExpressionUtils.evaluate(sourceExpression, this.invocation);
        }
        return this.invocation.getInstance();
    }

    public AzureString getDescription() {
        final AzureOperation annotation = this.invocation.getAnnotation(AzureOperation.class);
        final String name = StringUtils.firstNonBlank(annotation.name(), annotation.value());
        final String[] params = Arrays.stream(annotation.params()).map(e -> ExpressionUtils.interpret(e, this.invocation)).toArray(String[]::new);
        return OperationBundle.description(name, (Object[]) params);
    }
}
