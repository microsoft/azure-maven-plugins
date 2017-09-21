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
    static class Direction {
        static final String IN = "in";
        static final String OUT = "out";
    }

    protected String type = "";

    protected String name = "";

    protected String direction = "";

    protected String dataType = "";

    @JsonGetter
    public String getType() {
        return type;
    }

    @JsonGetter
    public String getName() {
        return name;
    }

    @JsonGetter
    public String getDirection() {
        return direction;
    }

    @JsonGetter
    public String getDataType() {
        return dataType;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected BaseBinding(final String name, final String type, final String direction, final String dataType) {
        this.name = name;
        this.type = type;
        this.direction = direction;
        this.dataType = dataType;
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
