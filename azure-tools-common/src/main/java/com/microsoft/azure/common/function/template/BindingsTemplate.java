/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.template;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Arrays;
import java.util.Map;

// This is the json template class correspond to bindings.json
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BindingsTemplate {
    private String schema;
    private String contentVersion;
    private Map<String, String> variables;
    private BindingTemplate[] bindings;

    @JsonGetter(value = "$schema")
    public String getSchema() {
        return schema;
    }

    @JsonGetter
    public String getContentVersion() {
        return contentVersion;
    }

    @JsonGetter
    public Map<String, String> getVariables() {
        return variables;
    }

    @JsonGetter
    public BindingTemplate[] getBindings() {
        return bindings;
    }

    @JsonSetter(value = "$schema")
    public void setSchema(String schema) {
        this.schema = schema;
    }

    @JsonSetter
    public void setContentVersion(String contentVersion) {
        this.contentVersion = contentVersion;
    }

    @JsonSetter
    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    @JsonSetter
    public void setBindings(BindingTemplate[] bindings) {
        this.bindings = bindings;
    }

    public BindingTemplate getBindingTemplateByName(String triggerType) {
        return Arrays.stream(this.bindings)
            .filter(binding -> binding.getType().equals(triggerType))
            .findFirst()
            .orElse(null);
    }
}
