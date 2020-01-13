/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.template;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionTemplate {
    private TemplateMetadata metadata;

    private Map<String, String> files;

    @JsonDeserialize(using = TemplateTriggerTypeDeserializer.class)
    private String function;

    @JsonGetter
    public TemplateMetadata getMetadata() {
        return metadata;
    }

    @JsonSetter
    public void setMetadata(TemplateMetadata metadata) {
        this.metadata = metadata;
    }

    @JsonGetter
    public Map<String, String> getFiles() {
        return files;
    }

    @JsonSetter
    public void setFiles(Map<String, String> files) {
        this.files = files;
    }

    @JsonGetter
    public String getFunction() {
        return function;
    }

    @JsonSetter
    public void setFunction(String function) {
        this.function = function;
    }

    public String getTriggerType() {
        return this.function;
    }
}
