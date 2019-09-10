/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.common.prompt;

public class InputValidateResult<T> {
    private T obj;
    private String errorMessage;

    public T getObj() {
        return obj;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static <T> InputValidateResult<T> wrap(T obj) {
        final InputValidateResult<T> res = new InputValidateResult<>();
        res.obj = obj;
        return res;
    }

    public static <T> InputValidateResult<T> error(String errorMessage) {
        final InputValidateResult<T> res = new InputValidateResult<>();
        res.errorMessage = errorMessage;
        return res;
    }
}
