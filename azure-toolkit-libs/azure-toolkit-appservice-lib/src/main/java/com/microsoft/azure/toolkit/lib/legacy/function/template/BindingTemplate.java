/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

// This is the json template class correspond to (bindings.json).bindings
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BindingTemplate {
    private String type;
    private String displayName;
    private String direction;
    private boolean enabledInTryMode;
    private FunctionSettingTemplate[] settings;

    public FunctionSettingTemplate getSettingTemplateByName(String name) {
        return Arrays.stream(settings).filter(template -> template.getName().equals(name)).findFirst().orElse(null);
    }
}
