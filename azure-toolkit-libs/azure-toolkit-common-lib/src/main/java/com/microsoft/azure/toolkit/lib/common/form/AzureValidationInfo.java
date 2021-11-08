/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AzureValidationInfo {
    private final Object value;
    private final AzureFormInput<?> input;
    private final String message;
    @Builder.Default
    private final Type type = Type.ERROR;

    public enum Type {
        // don't change the order, see `AzureForm#doValidate`
        PENDING, ERROR, WARNING, SUCCESS
    }

    public static AzureValidationInfo pending(AzureFormInput<?> input) {
        return AzureValidationInfo.builder().type(Type.PENDING).message("Validating...").input(input).build();
    }

    public static AzureValidationInfo error(String message, AzureFormInput<?> input) {
        return AzureValidationInfo.builder().type(Type.ERROR).message(message).input(input).build();
    }

    public static AzureValidationInfo warning(String message, AzureFormInput<?> input) {
        return AzureValidationInfo.builder().type(Type.WARNING).message(message).input(input).build();
    }

    public static AzureValidationInfo success(AzureFormInput<?> input) {
        return AzureValidationInfo.builder().type(Type.SUCCESS).message("Validation passed!").input(input).build();
    }

    public static AzureValidationInfo ok(AzureFormInput<?> input) {
        return success(input);
    }

    public static AzureValidationInfo none(AzureFormInput<?> input) {
        return AzureValidationInfo.builder().type(Type.SUCCESS).message("No need to validate.").input(input).build();
    }

    public boolean isValid() {
        return this.getType() != Type.PENDING && this.getType() != Type.ERROR;
    }
}
