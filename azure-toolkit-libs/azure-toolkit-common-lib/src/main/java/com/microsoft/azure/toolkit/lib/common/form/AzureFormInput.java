/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import com.microsoft.azure.toolkit.lib.common.DataStore;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutableTriple;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    /**
     * @throws RuntimeException if can not get a valid value.
     */
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
        return new ArrayList<>(this.get(FIELD_VALUE_LISTENERS, new CopyOnWriteArrayList<>()));
    }

    /**
     * @return {@code true} if event is really fired(when value is really changed from last time), {@code false} otherwise
     */
    default boolean fireValueChangedEvent(T val) {
        final T value = this.get(FIELD_VALUE);
        final boolean changed = !Objects.equals(val, value);
        if (changed) {
            this.set(FIELD_VALUE, val);
            this.getValueChangedListeners().forEach(l -> l.accept(val));
        } else {
            // reset input component extension, see AzureTextInput#onDocumentChanged
            this.setValidationInfo(this.getValidationInfo());
        }
        return changed;
    }

    /**
     * @return {@code true} if event is really fired(when value is really changed from last time), {@code false} otherwise
     */
    default boolean fireValueChangedEvent() {
        final Field<MutableTriple<T, Mono<AzureValidationInfo>, Disposable>> VALIDATING = Field.of(FIELD_VALIDATING);
        final MutableTriple<T, Mono<AzureValidationInfo>, Disposable> validating = this.get(VALIDATING);
        T value = null;
        try {
            value = this.getValue(); // parsing value may throw exception
        } catch (Exception e) {
            Optional.ofNullable(validating).ifPresent(v -> v.getRight().dispose());
            final String msg = StringUtils.isBlank(e.getMessage()) ? "invalid value." : e.getMessage();
            final AzureValidationInfo info = AzureValidationInfo.error(msg, this);
            this.setValidationInfo(info);
        }
        return this.fireValueChangedEvent(value);
    }

    default String getLabel() {
        return this.getClass().getSimpleName();
    }

    /**
     * override this method to implement validation
     *
     * @deprecated use {@link #addValidator(Validator)} instead.
     */
    @Nonnull
    @Deprecated
    default AzureValidationInfo doValidate(T value) {
        return AzureValidationInfo.none(this);
    }

    default AzureValidationInfo validateInternal(T v) {
        if (this.isRequired() && ObjectUtils.isEmpty(v)) {
            final String message = StringUtils.isEmpty(this.getLabel()) ?
                MSG_REQUIRED : String.format("\"%s\" is required.", this.getLabel());
            return AzureValidationInfo.error(message, this);
        } else {
            AzureValidationInfo result = null;
            final Collection<Validator> validators = this.getValidators();
            validators.add(() -> this.doValidate(v));
            for (Validator validator : validators) {
                AzureValidationInfo info;
                try {
                    info = ObjectUtils.firstNonNull(validator.doValidate(), AzureValidationInfo.none(this));
                } catch (Exception e) {
                    info = AzureValidationInfo.error(e.getMessage(), this);
                }
                if (result == null || info.getType().ordinal() < result.getType().ordinal()) {
                    result = info;
                }
                if (!info.isValid()) {
                    break;
                }
            }
            return ObjectUtils.firstNonNull(result, AzureValidationInfo.none(this));
        }
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
            final Field<MutableTriple<T, Mono<AzureValidationInfo>, Disposable>> VALIDATING = Field.of(FIELD_VALIDATING);
            final MutableTriple<T, Mono<AzureValidationInfo>, Disposable> validating = this.get(VALIDATING);
            T value;
            try {
                value = this.getValue(); // parsing value may throw exception
            } catch (Exception e) {
                Optional.ofNullable(validating).ifPresent(v -> v.getRight().dispose());
                final String msg = StringUtils.isBlank(e.getMessage()) ? "invalid value." : e.getMessage();
                final AzureValidationInfo info = AzureValidationInfo.error(msg, this);
                this.setValidationInfo(info);
                this.set(VALIDATING, null);
                return Mono.just(info);
            }
            if (Objects.nonNull(validating)) {
                if (Objects.equals(validating.getLeft(), value)) {
                    return validating.getMiddle();
                } else if (!validating.getRight().isDisposed()) {
                    validating.getRight().dispose();
                }
            }
            this.set(VALIDATING, null);
            if (!this.needValidation()) {
                final AzureValidationInfo info = AzureValidationInfo.none(this);
                this.setValidationInfo(info);
                return Mono.just(info);
            }
            return validateInternalAsync(value);
        }
    }

    @Nonnull
    default Mono<AzureValidationInfo> validateInternalAsync(final T value) {
        final Field<MutableTriple<T, Mono<AzureValidationInfo>, Disposable>> VALIDATING = Field.of(FIELD_VALIDATING);
        final MutableTriple<T, Mono<AzureValidationInfo>, Disposable> validating = MutableTriple.of(value, null, null);
        this.setValidationInfo(AzureValidationInfo.pending(this));
        final AzureString title = AzureString.format("validating \"%s\"...", this.getLabel());
        final Mono<AzureValidationInfo> flux = Mono.just(Optional.ofNullable(value)) // value may be null
            .publishOn(Schedulers.fromExecutor(command -> AzureTaskManager.getInstance().runInBackground(new AzureTask<>(title, command))))
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

    default void setValidationInfo(@Nullable AzureValidationInfo info) {
        synchronized (this) {
            this.set(FIELD_VALIDATION_INFO, info);
        }
    }

    /**
     * @return last saved validation info
     */
    default AzureValidationInfo getValidationInfo() {
        synchronized (this) {
            return this.get(FIELD_VALIDATION_INFO);
        }
    }

    /**
     * @return last saved validation info or validate asynchronously ({@code PENDING} before validation completes)
     */
    default AzureValidationInfo getValidationInfo(final boolean revalidateIfNone) {
        synchronized (this) {
            final AzureValidationInfo info = this.getValidationInfo();
            if (revalidateIfNone && Objects.isNull(info)) {
                this.validateValueAsync();
            }
            return this.getValidationInfo();
        }
    }

    @Nonnull
    default Collection<Validator> getValidators() {
        return new ArrayList<>(this.get(FIELD_VALIDATORS, new ArrayList<>()));
    }

    @Deprecated
    default void setValidator(Validator validator) {
        this.set(FIELD_VALIDATORS, Arrays.asList(validator));
    }

    default void addValidator(Validator validator) {
        final Collection<Validator> validators = this.get(FIELD_VALIDATORS, new ArrayList<>());
        validators.add(validator);
    }

    default boolean isRequired() {
        return this.get(FIELD_REQUIRED, false);
    }

    default void setRequired(boolean required) {
        this.set(FIELD_REQUIRED, required);
    }

    default boolean needValidation() {
        return this.isRequired() || !this.getValidators().isEmpty();
    }

    @FunctionalInterface
    interface Validator {
        AzureValidationInfo doValidate();
    }
}
