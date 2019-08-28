/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.prompt;

public class InputValidationResult<T> {
    T obj;
    String errorMessage;

    public T getObj() {
        return obj;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static <T> InputValidationResult<T> wrap(T obj) {
        final InputValidationResult<T> res = new InputValidationResult<>();
        res.obj = obj;
        return res;
    }

    public static <T> InputValidationResult<T> error(String errorMessage) {
        final InputValidationResult<T> res = new InputValidationResult<>();
        res.errorMessage = errorMessage;
        return res;
    }
}
