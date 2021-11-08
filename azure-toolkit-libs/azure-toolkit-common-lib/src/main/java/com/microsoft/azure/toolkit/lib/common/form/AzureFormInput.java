/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import com.microsoft.azure.toolkit.lib.common.DataStore;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutableTriple;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public interface AzureFormInput<T> extends DataStore {
    String MSG_REQUIRED = "This field is required.";
    String FIELD_VALUE = "value";
    String FIELD_VALIDATORS = "validators";
    String FIELD_REQUIRED = "required";
    String FIELD_VALIDATION_INFO = "validationInfo";
    String FIELD_VALUE_LISTENERS = "valueListeners";
    String FIELD_TRACKING = "tracking";
    String FIELD_VALIDATING = "validating";

    default T getValue() {
        return this.get(FIELD_VALUE);
    }

    default void setValue(final T val) {
        this.set(FIELD_VALUE, val);
    }

    default void addValueChangedListener(Consumer<T> listener) {
        final CopyOnWriteArrayList<Consumer<T>> valueChangedListeners = this.get(FIELD_VALUE_LISTENERS, new CopyOnWriteArrayList<>());
        if (!valueChangedListeners.contains(listener)) {
            valueChangedListeners.add(listener);
        }
    }

    default void removeValueChangedListener(Consumer<T> listener) {
        final CopyOnWriteArrayList<Consumer<T>> valueChangedListeners = this.get(FIELD_VALUE_LISTENERS, new CopyOnWriteArrayList<>());
        valueChangedListeners.remove(listener);
    }

    default List<Consumer<T>> getValueChangedListeners() {
        return this.get(FIELD_VALUE_LISTENERS, new CopyOnWriteArrayList<>());
    }

    default void fireValueChangedEvent(T val) {
        final T value = this.get(FIELD_VALUE);
        if (!Objects.equals(val, value)) {
            this.set(FIELD_VALUE, val);
            this.getValueChangedListeners().forEach(l -> l.accept(val));
        }
    }

    default String getLabel() {
        return this.getClass().getSimpleName();
    }

    /**
     * override this method to implement validation
     */
    @Nonnull
    default AzureValidationInfo doValidate(T value) {
        return AzureValidationInfo.none(this);
    }

    default AzureValidationInfo validateInternal(T v) {
        final Collection<Validator> validators = this.getValidators();
        if (this.isRequired() && ObjectUtils.isEmpty(v)) {
            final String message = StringUtils.isEmpty(this.getLabel()) ?
                MSG_REQUIRED : String.format("\"%s\" is required.", this.getLabel());
            return AzureValidationInfo.error(message, this);
        } else if (CollectionUtils.isNotEmpty(validators)) {
            for (Validator validator : validators) {
                final AzureValidationInfo info = validator.doValidate();
                if (info.isValid()) {
                    return info;
                }
            }
        }
        return doValidate(v);
    }

    /**
     * validate and set validation info
     *
     * @return the validation result
     */
    @Nonnull
    default AzureValidationInfo validateValue() {
        return this.validateValueAsync().block();
    }

    /**
     * validate and set validation info
     *
     * @return the validation result
     */
    default Mono<AzureValidationInfo> validateValueAsync() {
        synchronized (this) {
            final T value = this.getValue();
            final Field<MutableTriple<T, Mono<AzureValidationInfo>, Disposable>> VALIDATING = Field.of(FIELD_VALIDATING);
            final MutableTriple<T, Mono<AzureValidationInfo>, Disposable> validating = this.get(VALIDATING, MutableTriple.of(value, null, null));
            if (Objects.nonNull(validating.getMiddle()) && Objects.equals(validating.getLeft(), value)) {
                return validating.getMiddle();
            } else if (Objects.nonNull(validating.getRight())) {
                validating.getRight().dispose();
            }
            this.setValidationInfo(AzureValidationInfo.pending(this));
            final Mono<AzureValidationInfo> flux = Mono.just(Optional.ofNullable(value)) // value may be null
                .publishOn(Schedulers.fromExecutor(command -> AzureTaskManager.getInstance().runOnPooledThread(command)))
                .map(ov -> this.validateInternal(ov.orElse(null)))
                .doFinally(s -> {
                    if (Objects.equals(validating, this.get(VALIDATING))) {
                        this.set(VALIDATING, null);
                    }
                }).share();
            validating.setMiddle(flux);
            validating.setRight(flux.subscribe(info -> {
                if (Objects.equals(value, this.getValue())) {
                    this.setValidationInfo(info);
                }
            }));
            this.set(VALIDATING, validating);
            return flux;
        }
    }

    default void trackValidation() {
        synchronized (this) {
            final Field<Consumer<T>> TRACKING = Field.of(FIELD_TRACKING);
            Consumer<T> tracking = this.get(TRACKING);
            if (Objects.isNull(tracking)) {
                tracking = v -> {
                    this.validateValueAsync();
                };
                this.addValueChangedListener(tracking);
                this.set(TRACKING, tracking);
            }
        }
    }

    default void setValidationInfo(AzureValidationInfo info) {
        this.set(FIELD_VALIDATION_INFO, info);
    }

    /**
     * @return last saved validation info
     */
    default AzureValidationInfo getValidationInfo() {
        return this.get(FIELD_VALIDATION_INFO);
    }

    /**
     * @return last saved validation info or validate asynchronously ({@code PENDING} before validation completes)
     */
    default AzureValidationInfo getValidationInfo(final boolean revalidateIfNone) {
        final AzureValidationInfo info = this.getValidationInfo();
        if (revalidateIfNone && Objects.isNull(info)) {
            this.validateValueAsync();
        }
        return this.getValidationInfo();
    }

    @Nonnull
    default Collection<Validator> getValidators() {
        return this.get(FIELD_VALIDATORS, new ArrayList<>());
    }

    @Deprecated
    default void setValidator(Validator validator) {
        this.set(FIELD_VALIDATORS, Arrays.asList(validator));
    }

    default void addValidator(Validator validator) {
        final Collection<Validator> validators = this.getValidators();
        validators.add(validator);
    }

    default boolean isRequired() {
        return this.get(FIELD_REQUIRED, false);
    }

    default void setRequired(boolean required) {
        this.set(FIELD_REQUIRED, required);
    }

    @FunctionalInterface
    interface Validator {
        AzureValidationInfo doValidate();
    }
}
