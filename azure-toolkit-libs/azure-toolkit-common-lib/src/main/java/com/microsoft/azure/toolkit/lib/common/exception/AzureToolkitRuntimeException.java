/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

import lombok.Getter;

@Getter
public class AzureToolkitRuntimeException extends RuntimeException {
    private final String action;
    private final String actionId;

    public AzureToolkitRuntimeException(String error) {
        this(error, null, null);
    }

    public AzureToolkitRuntimeException(String error, Throwable cause) {
        this(error, cause, null);
    }

    public AzureToolkitRuntimeException(String error, String action) {
        this(error, null, action);
    }

    public AzureToolkitRuntimeException(String error, Throwable cause, String action) {
        this(error, cause, action, null);
    }

    public AzureToolkitRuntimeException(String error, Throwable cause, String action, String actionId) {
        super(error, cause);
        this.action = action;
        this.actionId = actionId;
    }
}
