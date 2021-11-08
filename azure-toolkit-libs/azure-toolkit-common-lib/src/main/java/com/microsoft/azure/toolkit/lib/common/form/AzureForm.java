/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.form;

import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public interface AzureForm<T> extends AzureFormInput<T> {
    T getValue();

    void setValue(T data);

    @Override
    @Nonnull
    default AzureValidationInfo validateInternal(T value) {
        final List<AzureValidationInfo> infos = this.validateAllInputs();
        assert infos.size() == this.getInputs().size() : "some inputs have no validation info.";
        final AzureValidationInfo info = infos.stream()
            .min(Comparator.comparing(AzureValidationInfo::getType))
            .orElse(AzureValidationInfo.pending(this));
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
        return this.getInputs().stream()
            .map(input -> input.getValidationInfo(revalidateIfNone))
            .filter(Objects::nonNull).collect(Collectors.toList());
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
