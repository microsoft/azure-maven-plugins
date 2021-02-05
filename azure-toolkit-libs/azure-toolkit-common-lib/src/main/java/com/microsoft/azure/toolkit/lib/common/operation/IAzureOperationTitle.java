/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface IAzureOperationTitle {
    String getName();

    Object[] getParams();

    static IAzureOperationTitle fromTitle(String title) {
        return new Simple(title);
    }

    @Getter
    @RequiredArgsConstructor
    class Simple implements IAzureOperationTitle {
        private final String title;
        private final String name = null;
        private final Object[] params = new Object[0];

        @Override
        public String toString() {
            return title;
        }
    }
}
