/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.template;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TemplateMetadata {
    private String defaultFunctionName;

    private String description;

    private String name;

    private String language;

    private List<String> category;

    private boolean enabledInTryMode;

    private List<String> userPrompt;

    @JsonGetter
    public String getDefaultFunctionName() {
        return defaultFunctionName;
    }

    @JsonSetter
    public void setDefaultFunctionName(String defaultFunctionName) {
        this.defaultFunctionName = defaultFunctionName;
    }

    @JsonGetter
    public String getDescription() {
        return description;
    }

    @JsonSetter
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonGetter
    public String getName() {
        return name;
    }

    @JsonSetter
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter
    public String getLanguage() {
        return language;
    }

    @JsonSetter
    public void setLanguage(String language) {
        this.language = language;
    }

    @JsonGetter
    public List<String> getCategory() {
        return category;
    }

    @JsonSetter
    public void setCategory(List<String> category) {
        this.category = category;
    }

    @JsonGetter
    public boolean isEnabledInTryMode() {
        return enabledInTryMode;
    }

    @JsonSetter
    public void setEnabledInTryMode(boolean enabledInTryMode) {
        this.enabledInTryMode = enabledInTryMode;
    }

    @JsonGetter
    public List<String> getUserPrompt() {
        return userPrompt;
    }

    @JsonSetter
    public void setUserPrompt(List<String> userPrompt) {
        this.userPrompt = userPrompt;
    }
}
