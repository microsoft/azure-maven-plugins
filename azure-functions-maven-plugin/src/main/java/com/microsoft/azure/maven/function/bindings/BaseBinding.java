/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class BaseBinding {

    protected String type = "";

    protected String name = "";

    protected String direction = "";

    @JsonGetter("type")
    public String getType() {
        return type;
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonGetter("direction")
    public String getDirection() {
        return direction;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("[ name: ")
                .append(getName())
                .append(", type: ")
                .append(getType())
                .append(", direction: ")
                .append(getDirection())
                .append(" ]")
                .toString();
    }
}
