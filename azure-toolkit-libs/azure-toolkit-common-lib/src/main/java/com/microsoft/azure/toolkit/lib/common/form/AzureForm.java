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
import java.util.stream.Stream;

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
        return Flux.fromIterable(this.getInputs()).flatMap(i -> i.validateValueAsync().flux()).concatWith(Flux.fromIterable(this.validateAdditionalInfo()));
    }

    default List<AzureValidationInfo> getAllValidationInfos(final boolean revalidateIfNone) {
        return Stream.concat(this.getInputs().stream().map(input -> input.getValidationInfo(revalidateIfNone)), this.validateAdditionalInfo().stream())
            .filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    default AzureValidationInfo getValidationInfo() {
        return this.getAllValidationInfos(true).stream()
            .filter(i -> i.getType() != SUCCESS)
            .min(Comparator.comparing(AzureValidationInfo::getType))
            .orElse(AzureValidationInfo.ok(this));
    }

    default boolean needValidation() {
        return CollectionUtils.isNotEmpty(this.getInputs());
    }

    List<AzureFormInput<?>> getInputs();

    /**
     * validate additional info.
     * e.g. validate combination of inputs, such as password and confirm password
     */
    default List<AzureValidationInfo> validateAdditionalInfo() {
        return Collections.emptyList();
    }
}
