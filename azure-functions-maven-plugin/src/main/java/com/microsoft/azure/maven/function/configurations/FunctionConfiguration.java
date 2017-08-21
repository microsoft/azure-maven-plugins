/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.maven.function.bindings.BaseBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema of function.json is at https://github.com/Azure/azure-webjobs-sdk-script/blob/dev/schemas/json/function.json
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FunctionConfiguration {
    public static final String MULTIPLE_TRIGGER = "Only one trigger is allowed for each Azure Function. " +
            "Multiple triggers found on method: ";
    public static final String HTTP_OUTPUT_NOT_ALLOWED = "HttpOutput binding is only allowed to use with " +
            "HttpTrigger binding. HttpOutput binding found on method: ";

    private String scriptFile;

    private String entryPoint;

    private List<BaseBinding> bindings = new ArrayList<>();

    private boolean disabled = false;

    private boolean excluded;

    @JsonGetter("scriptFile")
    public String getScriptFile() {
        return scriptFile;
    }

    @JsonGetter("entryPoint")
    public String getEntryPoint() {
        return entryPoint;
    }

    @JsonGetter("bindings")
    public List<BaseBinding> getBindings() {
        return bindings;
    }

    @JsonGetter("disabled")
    public boolean isDisabled() {
        return disabled;
    }

    @JsonGetter("excluded")
    public boolean isExcluded() {
        return excluded;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }

    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public void validate() {
        if (getBindings().stream().filter(b -> b.getType().endsWith("Trigger")).count() > 1) {
            throw new RuntimeException(MULTIPLE_TRIGGER + getEntryPoint());
        }

        if (getBindings().stream().noneMatch(b -> b.getType().equalsIgnoreCase("httpTrigger")) &&
                getBindings().stream().anyMatch(b -> b.getType().equalsIgnoreCase("http"))) {
            throw new RuntimeException(HTTP_OUTPUT_NOT_ALLOWED + getEntryPoint());
        }
    }
}
