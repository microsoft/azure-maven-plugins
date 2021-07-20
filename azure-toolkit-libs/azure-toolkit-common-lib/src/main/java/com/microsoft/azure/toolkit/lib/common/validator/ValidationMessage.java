/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.common.validator;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.networknt.schema.ValidatorTypeCode;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@Getter
@Builder(toBuilder = true)
public class ValidationMessage {
    private String type;
    private String code;
    private String path;
    private String[] arguments;
    private AzureString message;

    static ValidationMessage fromRawMessage(com.networknt.schema.ValidationMessage validationMessage) {
        return ValidationMessage.builder()
                .type(validationMessage.getType())
                .code(validationMessage.getCode())
                .path(validationMessage.getPath())
                .arguments(validationMessage.getArguments())
                .message(getMessage(validationMessage)).build();
    }

    // refers https://github.com/networknt/json-schema-validator/blob/master/src/main/java/com/networknt/schema/ValidationMessage.java#L174
    private static AzureString getMessage(final com.networknt.schema.ValidationMessage validationMessage) {
        final ValidatorTypeCode validatorTypeCode = Arrays.stream(ValidatorTypeCode.values())
                .filter(typeCode -> StringUtils.equalsIgnoreCase(validationMessage.getCode(), typeCode.getErrorCode())).findFirst().orElse(null);
        if (validatorTypeCode == null) {
            return AzureString.fromString(validationMessage.getMessage());
        }
        final String[] path = new String[]{validationMessage.getPath()};
        final String[] args = ArrayUtils.isEmpty(validationMessage.getArguments()) ? path : ArrayUtils.addAll(path, validationMessage.getArguments());
        return AzureString.format(validatorTypeCode.getMessageFormat().toPattern(), (String[]) args);
    }
}
