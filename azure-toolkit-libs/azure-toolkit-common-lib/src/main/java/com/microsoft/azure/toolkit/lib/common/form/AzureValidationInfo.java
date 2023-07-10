/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
@Builder
public class AzureValidationInfo {
    private final Object value;
    /**
     * AzureFormInput or JComponent
     */
    private final Object input;
    private final String message;
    @Builder.Default
    private final Type type = Type.ERROR;

    public enum Type {
        // don't change the order, see `AzureForm#doValidate`
        PENDING, ERROR, WARNING, SUCCESS
    }

    /**
     * @param input the input (AzureFormInput or JComponent) to be validated
     */
    public static AzureValidationInfo pending(Object input) {
        return AzureValidationInfo.builder().type(Type.PENDING).message("Validating...").input(input).build();
    }

    /**
     * @param input the input (AzureFormInput or JComponent) to be validated
     */
    public static AzureValidationInfo error(@Nonnull String message, Object input) {
        return AzureValidationInfo.builder().type(Type.ERROR).message(message).input(input).build();
    }

    /**
     * @param input the input (AzureFormInput or JComponent) to be validated
     */
    public static AzureValidationInfo warning(@Nonnull String message, Object input) {
        return AzureValidationInfo.builder().type(Type.WARNING).message(message).input(input).build();
    }

    /**
     * @param input the input (AzureFormInput or JComponent) to be validated
     */
    public static AzureValidationInfo success(Object input) {
        return AzureValidationInfo.builder().type(Type.SUCCESS).message("Validation passed!").input(input).build();
    }

    /**
     * @param input the input (AzureFormInput or JComponent) to be validated
     */
    public static AzureValidationInfo ok(Object input) {
        return success(input);
    }

    /**
     * @param input the input (AzureFormInput or JComponent) to be validated
     */
    public static AzureValidationInfo none(Object input) {
        return AzureValidationInfo.builder().type(Type.SUCCESS).message("No need to validate.").input(input).build();
    }

    public boolean isValid() {
        return this.getType() != Type.PENDING && this.getType() != Type.ERROR;
    }

    @Override
    public String toString() {
        return String.format("[%s]%s", this.type, this.message);
    }
}
