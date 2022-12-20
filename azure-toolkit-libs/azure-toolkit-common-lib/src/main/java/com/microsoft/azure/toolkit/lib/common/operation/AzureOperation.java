/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AzureOperation {

    /**
     * alias for {@link #name()}
     */
    String value() default "";

    String name() default "";

    /**
     * groovy expressions to compute the params dynamically.
     * e.g. groovy expression: {@code "this.webapp.id()" }, {@code "subscriptionId" }
     */
    String[] params() default {};
}
