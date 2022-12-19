/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import org.apache.commons.collections4.CollectionUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type.SUCCESS;

public interface AzureForm<T> extends AzureFormInput<T> {
    T getValue();

    void setValue(T data);

    @Override
    @Nonnull
    default AzureValidationInfo validateInternal(T value) {
        final AzureValidationInfo info = this.getValidationInfo();
        this.setValidationInfo(info);
        return info;
    }

    default List<AzureValidationInfo> validateAllInputs() {
        return this.validateAllInputsAsync().collectList().block();
    }

    default Flux<AzureValidationInfo> validateAllInputsAsync() {
        return Flux.fromIterable(this.getInputs()).flatMap(i -> i.validateValueAsync().flux());
    }

    default List<AzureValidationInfo> getAllValidationInfos(final boolean revalidateIfNone) {
        return this.getInputs().stream().map(input -> input.getValidationInfo(revalidateIfNone))
            .filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    default AzureValidationInfo getValidationInfo() {
        return this.getInputs().stream().map(input -> input.getValidationInfo(true))
            .filter(i -> i.getType() != SUCCESS)
            .min(Comparator.comparing(AzureValidationInfo::getType))
            .orElse(AzureValidationInfo.ok(this));
    }

    default boolean needValidation() {
        return CollectionUtils.isNotEmpty(this.getInputs());
    }

    List<AzureFormInput<?>> getInputs();

    /**
     * use {@link #validateAllInputs()} instead
     */
    @Deprecated
    default List<AzureValidationInfo> validateData() {
        return this.validateAllInputs();
    }
}
