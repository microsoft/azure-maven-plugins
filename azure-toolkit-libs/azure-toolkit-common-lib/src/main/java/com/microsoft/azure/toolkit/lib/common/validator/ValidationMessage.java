/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.common.validator;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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
                .message(getMessage(validationMessage.getMessage(), validationMessage.getPath(), validationMessage.getArguments())).build();
    }

    private static AzureString getMessage(final String rawMessage, final String path, final String[] arguments) {
        int parameterCount = 0;
        String pattern = rawMessage;
        if (StringUtils.isNotEmpty(path)) {
            pattern = pattern.replace(path, String.format("{%d}", parameterCount++));
        }
        if (ArrayUtils.isNotEmpty(arguments)) {
            for (String argument : arguments) {
                pattern = pattern.replace(argument, String.format("{%d}", parameterCount++));
            }
        }
        final String[] args = path == null ? arguments : ArrayUtils.addAll(new String[]{path}, arguments);
        return AzureString.format(pattern, (String[]) args);
    }
}
