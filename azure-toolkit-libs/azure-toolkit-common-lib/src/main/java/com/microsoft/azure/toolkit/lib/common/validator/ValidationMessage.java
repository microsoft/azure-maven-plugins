/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.common.validator;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder(toBuilder = true)
public class ValidationMessage {
    private String type;
    private String code;
    private String path;
    private String[] arguments;
    private Map<String, Object> details;
    private String rawMessage;

    static ValidationMessage fromRawMessage(com.networknt.schema.ValidationMessage validationMessage) {
        return ValidationMessage.builder()
                .type(validationMessage.getType())
                .code(validationMessage.getCode())
                .path(validationMessage.getPath())
                .arguments(validationMessage.getArguments())
                .details(validationMessage.getDetails())
                .rawMessage(validationMessage.getMessage()).build();
    }
}
