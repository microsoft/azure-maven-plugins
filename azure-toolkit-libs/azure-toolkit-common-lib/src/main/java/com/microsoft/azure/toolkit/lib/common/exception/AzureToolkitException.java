/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
public class AzureToolkitException extends Exception {
    /**
     * array of action ids or
     * {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     * {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    private final Object[] actions;
    @Nullable
    private final String tips;

    public AzureToolkitException(String error) {
        this(error, (Object[]) null);
    }

    public AzureToolkitException(String error, Throwable cause) {
        this(error, cause, (Object[]) null);
    }

    /**
     * @param actions array of action ids or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public AzureToolkitException(String error, Object... actions) {
        this(error, (Throwable) null, actions);
    }

    /**
     * @param actions array of action ids or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public AzureToolkitException(String error, Throwable cause, Object... actions) {
        super(error, cause);
        this.actions = actions;
        this.tips = null;
    }

    public AzureToolkitException(String error, @Nonnull String tips) {
        this(error, tips, (Object[]) null);
    }

    public AzureToolkitException(String error, Throwable cause, @Nonnull String tips) {
        this(error, cause, tips, (Object[]) null);
    }

    /**
     * @param actions array of action ids or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public AzureToolkitException(String error, @Nonnull String tips, Object... actions) {
        this(error, null, tips, actions);
    }

    /**
     * @param actions array of action ids or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public AzureToolkitException(String error, Throwable cause, @Nonnull String tips, Object... actions) {
        super(error, cause);
        this.actions = actions;
        this.tips = tips;
    }
}
