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

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionTemplates {
    private String schema;

    private String contentVersion;

    private List<FunctionTemplate> templates;

    @JsonGetter(value = "$schema")
    public String getSchema() {
        return schema;
    }

    @JsonSetter(value = "$schema")
    public void setSchema(String schema) {
        this.schema = schema;
    }

    @JsonGetter
    public String getContentVersion() {
        return contentVersion;
    }

    @JsonSetter
    public void setContentVersion(String contentVersion) {
        this.contentVersion = contentVersion;
    }

    @JsonGetter
    public List<FunctionTemplate> getTemplates() {
        return templates;
    }

    @JsonSetter
    public void setTemplates(List<FunctionTemplate> templates) {
        this.templates = templates;
    }
}
