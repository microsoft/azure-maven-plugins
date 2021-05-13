/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.Getter;

public interface ISqlServerDeleter<T> extends ICommittable<T> {

    ISqlServerDeleter<T> withId(String id);

    ISqlServerDeleter<T> withName(String name);

    ISqlServerDeleter<T> withResourceGroup(String resoureGroup);

    @Getter
    abstract class AbstractSqlServerDeleter<T> implements ISqlServerDeleter<T> {

        private String id;
        private String name;
        private String resourceGroup;

        @Override
        public ISqlServerDeleter<T> withId(String id) {
            this.id = id;
            return this;
        }

        @Override
        public ISqlServerDeleter<T> withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ISqlServerDeleter<T> withResourceGroup(String resourceGroup) {
            this.resourceGroup = resourceGroup;
            return this;
        }

    }
}
