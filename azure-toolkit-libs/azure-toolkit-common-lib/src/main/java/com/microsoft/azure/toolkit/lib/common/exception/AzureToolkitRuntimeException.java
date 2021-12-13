/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

import lombok.Getter;

@Getter
public class AzureToolkitRuntimeException extends RuntimeException {
    /**
     * array of action id or
     * {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     * {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    private final Object[] actions;

    public AzureToolkitRuntimeException(Throwable cause) {
        this(null, cause);
    }

    public AzureToolkitRuntimeException(String error) {
        this(error, (Object[]) null);
    }

    public AzureToolkitRuntimeException(String error, Throwable cause) {
        this(error, cause, (Object[]) null);
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public AzureToolkitRuntimeException(String error, Object... actions) {
        this(error, null, actions);
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public AzureToolkitRuntimeException(String error, Throwable cause, Object... actions) {
        super(error, cause);
        this.actions = actions;
    }
}
