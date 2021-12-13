/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

import lombok.Getter;

@Getter
public class AzureToolkitException extends Exception {
    /**
     * array of action id or
     * {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     * {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    private final Object[] actions;

    public AzureToolkitException(String error) {
        this(error, (Object[]) null);
    }

    public AzureToolkitException(String error, Throwable cause) {
        this(error, cause, (Object[]) null);
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public AzureToolkitException(String error, Object... actions) {
        this(error, null, actions);
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    public AzureToolkitException(String error, Throwable cause, Object... actions) {
        super(error, cause);
        this.actions = actions;
    }
}
