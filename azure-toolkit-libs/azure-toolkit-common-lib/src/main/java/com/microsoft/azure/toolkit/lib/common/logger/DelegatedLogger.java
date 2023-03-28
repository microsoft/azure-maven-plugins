/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.logger;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import java.util.Objects;

@RequiredArgsConstructor
class DelegatedLogger implements Logger {
    private final Class<?> clazz;
    private Logger delegate;

    @Delegate
    private Logger getDelegate() {
        if (Objects.isNull(delegate)) {
            synchronized (this) {
                if (Objects.isNull(delegate)) {
                    this.delegate = LoggerFactory.getInstance().getLogger(clazz);
                }
            }
        }
        return this.delegate;
    }
}
