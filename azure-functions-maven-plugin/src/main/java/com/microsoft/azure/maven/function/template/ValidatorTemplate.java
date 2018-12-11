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
public class ValidatorTemplate {
    private String expression;
    private String errorText;

    @JsonGetter
    public String getExpression() {
        return expression;
    }

    @JsonSetter
    public void setExpression(String expression) {
        this.expression = expression;
    }

    @JsonGetter
    public String getErrorText() {
        return errorText;
    }

    @JsonSetter
    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }
}
