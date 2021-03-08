/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder
@Data
public class AzureValidationInfo {
    public static final AzureValidationInfo PENDING =
        AzureValidationInfo.builder().type(Type.PENDING).message("PENDING").build();
    public static final AzureValidationInfo OK =
        AzureValidationInfo.builder().type(Type.INFO).message("OK").build();
    public static final AzureValidationInfo UNINITIALIZED =
        AzureValidationInfo.builder().type(Type.INFO).message("UNINITIALIZED").build();
    private final AzureFormInput<?> input;
    private final String message;
    @Builder.Default
    private final Type type = Type.ERROR;

    public enum Type {
        ERROR, WARNING, INFO, PENDING
    }
}
