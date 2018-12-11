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

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionSettingTemplate {
    private String name;
    private String value;
    private String defaultValue;
    private boolean required;
    private String label;
    private String help;
    private ValidatorTemplate[] validators;

    @JsonGetter
    public String getName() {
        return name;
    }

    @JsonGetter
    public String getValue() {
        return value;
    }

    @JsonGetter
    public String getDefaultValue() {
        return defaultValue;
    }

    @JsonGetter
    public boolean isRequired() {
        return required;
    }

    @JsonGetter
    public String getLabel() {
        return label;
    }

    @JsonGetter
    public String getHelp() {
        return help;
    }

    @JsonGetter
    public ValidatorTemplate[] getValidators() {
        return validators;
    }

    @JsonSetter
    public void setName(String name) {
        this.name = name;
    }

    @JsonSetter
    public void setValue(String value) {
        this.value = value;
    }

    @JsonSetter
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @JsonSetter
    public void setRequired(boolean required) {
        this.required = required;
    }

    @JsonSetter
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonSetter
    public void setHelp(String help) {
        this.help = help;
    }

    @JsonSetter
    public void setValidators(ValidatorTemplate[] validators) {
        this.validators = validators;
    }

    public String getSettingRegex() {
        return (validators != null && validators.length > 0) ? validators[0].getExpression() : null;
    }

    public String getErrorText(){
        return (validators != null && validators.length > 0) ? validators[0].getErrorText() : null;
    }
}
