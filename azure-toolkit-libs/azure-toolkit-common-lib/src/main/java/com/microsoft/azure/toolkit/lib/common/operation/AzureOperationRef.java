/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.utils.aspect.ExpressionUtils;
import com.microsoft.azure.toolkit.lib.common.utils.aspect.MethodInvocation;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;

@Getter
@SuperBuilder
public class AzureOperationRef extends MethodInvocation implements IAzureOperation {

    @Override
    public String toString() {
        final AzureOperation annotation = this.getAnnotation(AzureOperation.class);
        return String.format("{title:'%s', method:%s}", annotation.name(), method.getName());
    }

    public String getName() {
        final AzureOperation annotation = this.getAnnotation(AzureOperation.class);
        return annotation.name();
    }

    public String getType() {
        final AzureOperation annotation = this.getAnnotation(AzureOperation.class);
        return annotation.type().name();
    }

    public String getTitle() {
        final AzureOperation annotation = this.getAnnotation(AzureOperation.class);
        final String name = annotation.name();
        final String[] params = Arrays.stream(annotation.params()).map(e -> ExpressionUtils.interpret(e, this)).toArray(String[]::new);
        return AzureOperationBundle.title(name, (Object[]) params).toString();
    }

    public String getId() {
        return Utils.getId(this);
    }
}
