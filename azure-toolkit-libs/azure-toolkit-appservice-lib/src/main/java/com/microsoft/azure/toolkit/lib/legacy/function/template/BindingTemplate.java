/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.template;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Arrays;

// This is the json template class correspond to (bindings.json).bindings
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BindingTemplate {
    private String type;
    private String displayName;
    private String direction;
    private boolean enabledInTryMode;
    private FunctionSettingTemplate[] settings;

    @JsonGetter
    public String getType() {
        return type;
    }

    @JsonGetter
    public String getDisplayName() {
        return displayName;
    }

    @JsonGetter
    public String getDirection() {
        return direction;
    }

    @JsonGetter
    public boolean isEnabledInTryMode() {
        return enabledInTryMode;
    }

    @JsonGetter
    public FunctionSettingTemplate[] getSettings() {
        return settings;
    }

    @JsonSetter
    public void setType(String type) {
        this.type = type;
    }

    @JsonSetter
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsonSetter
    public void setDirection(String direction) {
        this.direction = direction;
    }

    @JsonSetter
    public void setEnabledInTryMode(boolean enabledInTryMode) {
        this.enabledInTryMode = enabledInTryMode;
    }

    @JsonSetter
    public void setSettings(FunctionSettingTemplate[] settings) {
        this.settings = settings;
    }

    public FunctionSettingTemplate getSettingTemplateByName(String name) {
        return Arrays.stream(settings)
            .filter(template -> template.getName().equals(name))
            .findFirst().orElse(null);
    }
}
