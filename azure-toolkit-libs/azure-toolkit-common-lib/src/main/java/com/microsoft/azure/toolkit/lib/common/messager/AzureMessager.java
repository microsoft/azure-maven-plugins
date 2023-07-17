/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class AzureMessager implements IAzureMessager {
    private static IAzureMessager defaultMessager;

    public static synchronized void setDefaultMessager(@Nonnull IAzureMessager messager) {
        if (AzureMessager.defaultMessager == null) { // not allow overwriting...
            AzureMessager.defaultMessager = messager;
        }
    }

    @Nonnull
    public static IAzureMessager getDefaultMessager() {
        return Optional.ofNullable(AzureMessager.defaultMessager)
            .orElse(new DummyMessager());
    }

    @Nonnull
    public static IAzureMessager getMessager() {
        return Optional.ofNullable(OperationContext.current()).map(OperationContext::getMessager)
            .orElseGet(AzureMessager::getDefaultMessager);
    }

    @Slf4j
    public static class DummyMessager implements IAzureMessager {
        @Override
        public boolean show(IAzureMessage message) {
            log.info("DUMMY MESSAGE:{}", message);
            return false;
        }
    }
}
