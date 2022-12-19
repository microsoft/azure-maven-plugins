/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public class SystemException extends RuntimeException {
    /**
     * array of action ids or
     * {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     * {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    private final Object[] actions;

    public SystemException(@Nonnull Throwable cause) {
        this(null, cause);
    }

    public SystemException(@Nonnull String cause) {
        this(cause, (Object[]) null);
    }

    public SystemException(String error, @Nonnull Throwable cause) {
        this(error, cause, (Object[]) null);
    }

    /**
     * @param actions array of action ids or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public SystemException(@Nonnull String cause, Object... actions) {
        super(cause);
        this.actions = actions;
    }

    /**
     * @param actions array of action ids or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public SystemException(String error, @Nonnull Throwable cause, Object... actions) {
        super(error, cause);
        this.actions = actions;
    }
}
