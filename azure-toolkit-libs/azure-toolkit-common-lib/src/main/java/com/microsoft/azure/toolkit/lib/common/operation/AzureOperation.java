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
    String name();

    /**
     * expression supported, e.g. <code>{"@webapp", "$subsId"}</code>
     */
    String[] params() default {};

    /**
     * expression supported, e.g. <code>@buildProps($webapp)</code>
     */
    String props() default "";

    Type type() default Type.DEFAULT;

    enum Type {
        DEFAULT,

        /**
         * user triggered action, achieved by leveraging services and tasks
         * e.g. run configuration, download file
         */
        ACTION,

        /**
         * usually achieves certain biz goal(so it's biz related), similar to spring @Service, shared by actions and services
         * e.g. start a webapp(specified by id), update setting of deployment slot.
         */
        SERVICE,

        /**
         * finer granularity, shared by services and actions, lies at the same layer as spring @Repository
         * e.g. single io request, single db access, convert POJO to VO, ...
         */
        TASK,

        /**
         * a special type of task
         */
        REQUEST,
    }
}
