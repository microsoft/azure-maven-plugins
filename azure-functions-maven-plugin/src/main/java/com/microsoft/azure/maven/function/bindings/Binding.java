/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = BindingSerializer.class)
public class Binding {

    protected BindingEnum bindingEnum = null;

    protected String type = null;

    protected BindingEnum.Direction direction = null;

    protected Map<String, Object> bindingAttributes = new HashMap<>();

    protected static Map<BindingEnum, List<String>> requiredAttributeMap = new HashMap<>();

    static {
        //initialize required attributes, which will be saved to function.json even if it equals to its default value
        requiredAttributeMap.put(BindingEnum.EventHubTrigger, Arrays.asList("cardinality"));
    }

    public Binding(BindingEnum bindingEnum) {
        this.bindingEnum = bindingEnum;
        this.type = bindingEnum.getType();
        this.direction = bindingEnum.getDirection();
    }

    public Binding(BindingEnum bindingEnum, Annotation annotation) {
        this(bindingEnum);
        final Class<? extends Annotation> annotationType = annotation.annotationType();

        try {
            for (final Method method : annotationType.getDeclaredMethods()) {
                final Object value = method.invoke(annotation);
                addProperties(bindingEnum, value, method);
            }
        } catch (Exception e) {
            throw new RuntimeException("Resolving binding attributes failed", e);
        }
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return (String) bindingAttributes.get("name");
    }

    public String getDirection() {
        if (this.direction != null) {
            return direction.toString();
        }

        throw new RuntimeException("Direction must be provided.");
    }

    public BindingEnum getBindingEnum() {
        return bindingEnum;
    }

    public Object getAttribute(String attributeName) {
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

    protected void addProperties(BindingEnum binding, Object value, Method method) {
        final String propertyName = method.getName();
        if (propertyName.equals("direction") && value instanceof String) {
            this.direction = BindingEnum.Direction.fromString((String) value);
            return;
        }

        if (propertyName.equals("type") && value instanceof String) {
            this.type = (String) value;
            return;
        }

        final List<String> requiredAttributes = requiredAttributeMap.get(binding);
        if (!value.equals(method.getDefaultValue()) ||
                (requiredAttributes != null && requiredAttributes.contains(propertyName))) {
            bindingAttributes.put(propertyName, value);
        }

    }
}
