/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.template;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// This is the json template class correspond to (bindings.json).bindings.settings
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionSettingTemplate {
    private String name;
    private String value;
    private String resource;
    private String defaultValue;
    private boolean required;
    private String label;
    private String help;
    @JsonProperty(value = "emum")
    private SettingEnum[] settingEnum;
    private ValidatorTemplate[] validators;

    public String getSettingRegex() {
        return (validators != null && validators.length > 0) ? validators[0].getExpression() : null;
    }

    public String getErrorText() {
        return (validators != null && validators.length > 0) ? validators[0].getErrorText() : null;
    }

    @Data
    @JsonAutoDetect
    public static class SettingEnum {
        private String value;
        private String display;
    }
}
