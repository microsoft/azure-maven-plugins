/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public interface AzureFormInput<T> extends Validatable {

    String MSG_REQUIRED = "This field is required.";

    T getValue();

    void setValue(final T val);

    @Nonnull
    default AzureValidationInfo doValidate() {
        final T value = this.getValue();
        if (this.isRequired() && ObjectUtils.isEmpty(value)) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            String message = MSG_REQUIRED;
            if (!StringUtils.isEmpty(this.getLabel())) {
                message = String.format("\"%s\" is required.", this.getLabel());
            }
            return builder.message(message).input(this).type(AzureValidationInfo.Type.ERROR).build();
        }
        return Validatable.super.doValidate();
    }

    default boolean isRequired() {
        return false;
    }

    default String getLabel() {
        return "";
    }
}
