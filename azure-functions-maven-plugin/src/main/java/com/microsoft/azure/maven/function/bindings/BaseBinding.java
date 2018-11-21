/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = BindingSerializer.class)
public class BaseBinding {
    static class Direction {
        static final String IN = "in";
        static final String OUT = "out";
    }

    protected BindingEnum bindingEnum = null;

    protected String type = "";

    protected String direction = "";

    protected Map<String, Object> bindingAttributes = new HashMap<>();

    public BaseBinding(BindingEnum bindingEnum) {
        this.bindingEnum = bindingEnum;
        this.type = bindingEnum.getType();
        this.direction = bindingEnum.getDirection();
    }

    public BaseBinding(BindingEnum bindingEnum, Annotation annotation) {
        this(bindingEnum);
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        try {
            for (final Method method : annotationType.getDeclaredMethods()) {
                final Object value = method.invoke(annotation);
                if (!value.equals(method.getDefaultValue())) {
                    bindingAttributes.put(method.getName(), value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Binding attributes resolve failed");
        }
    }

    protected BaseBinding(final String name, final String type, final String direction, final String dataType) {
        this.type = type;
        this.direction = direction;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return (String) bindingAttributes.get("name");
    }

    public String getDirection() {
        return direction;
    }

    public String getDataType() {
        return (String) bindingAttributes.get("dataType");
    }

    public BindingEnum getBindingEnum() {
        return bindingEnum;
    }

    public Object getAttribute(String attributeName){
        return bindingAttributes.get(attributeName);
    }

    public Map<String, Object> getBindingAttributes() {
        return bindingAttributes;
    }

    public void setName(String name) {
        this.bindingAttributes.put("name", name);
    }

    public void setAttribute(String attributeName, Object attributeValue) {
        this.bindingAttributes.put(attributeName, attributeValue);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("[ name: ")
                .append(getName())
                .append(", type: ")
                .append(getType())
                .append(", direction: ")
                .append(getDirection())
                .append(" ]")
                .toString();
    }
}
