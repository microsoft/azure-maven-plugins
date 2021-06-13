/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IAzureMessage {
    @Nonnull
    String getMessage();

    @Nonnull
    Type getType();

    @Nullable
    String getTitle();

    @Nullable
    Object getPayload();

    @Nullable
    Action[] getActions();

    default boolean show() {
        return AzureMessager.getMessager().show(this);
    }

    default boolean show(IAzureMessager messager) {
        return messager.show(this);
    }

    enum Type {
        INFO, WARNING, SUCCESS, ERROR, ALERT, CONFIRM
    }

    interface Action {
        String name();

        void actionPerformed(IAzureMessage message);
    }
}
