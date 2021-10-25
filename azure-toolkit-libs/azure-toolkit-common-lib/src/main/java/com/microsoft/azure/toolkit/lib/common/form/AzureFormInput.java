/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import com.microsoft.azure.toolkit.lib.common.DataStore;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public interface AzureFormInput<T> extends Validatable, DataStore {

    String MSG_REQUIRED = "This field is required.";

    T getValue();

    void setValue(final T val);

    default void addValueChangedListener(Consumer<T> listener) {
        final CopyOnWriteArrayList<Consumer<T>> valueChangedListeners = this.get("valueChangedListeners", new CopyOnWriteArrayList<>());
        if (!valueChangedListeners.contains(listener)) {
            valueChangedListeners.add(listener);
        }
    }

    default void removeValueChangedListener(Consumer<T> listener) {
        final CopyOnWriteArrayList<Consumer<T>> valueChangedListeners = this.get("valueChangedListeners", new CopyOnWriteArrayList<>());
        valueChangedListeners.remove(listener);
    }

    default List<Consumer<T>> getValueChangedListeners() {
        return this.get("valueChangedListeners", new CopyOnWriteArrayList<>());
    }

    default void fireValueChangedEvent(T val) {
        this.getValueChangedListeners().forEach(l -> l.accept(val));
    }

    @Nonnull
    default AzureValidationInfo doValidate() {
        final T value = this.getValue();
        if (this.isRequired() && ObjectUtils.isEmpty(value)) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            String message = MSG_REQUIRED;
            if (!StringUtils.isEmpty(this.getLabel())) {
                message = String.format("\"%s\" is required.", this.getLabel());
            }
            return builder.message(message).input(this).type(AzureValidationInfo.Type.ERROR).build();
        }
        return Validatable.super.doValidate();
    }

    default String getLabel() {
        return this.getClass().getSimpleName();
    }

    @Nullable
    @Override
    default Validatable.Validator getValidator() {
        return this.get("validator");
    }

    default void setValidator(Validator validator) {
        this.set("validator", validator);
    }

    default boolean isRequired() {
        return this.get("required", false);
    }

    default void setRequired(boolean required) {
        this.set("required", required);
    }
}
