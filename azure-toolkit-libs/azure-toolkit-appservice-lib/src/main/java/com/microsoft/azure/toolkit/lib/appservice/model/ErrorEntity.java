/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode
public class ErrorEntity {
    private String extendedCode;
    private String messageTemplate;
    private List<String> parameters;
    private List<ErrorEntity> innerErrors;
    private List<ErrorEntity> details;
    private String target;
    private String code;
    private String message;
}
