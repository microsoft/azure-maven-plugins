/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import java.util.List;
import java.util.stream.Collectors;

public interface AzureForm<T> {
    T getData();

    void setData(T data);

    List<AzureFormInput<?>> getInputs();

    default List<AzureValidationInfo> validateData() {
        return this.getInputs().stream().map(AzureFormInput::doValidate).collect(Collectors.toList());
    }
}
