/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@EqualsAndHashCode
public class AccountModel {
    private String format;
    private String name;
    private String version;
    private String source;
    private List<ModelSku> skus;

    @Nullable
    public static AccountModel fromModel(@Nonnull final com.azure.resourcemanager.cognitiveservices.models.Model model) {
        return Optional.ofNullable(model.model()).map(AccountModel::fromModel).orElse(null);
    }

    @Nonnull
    public static AccountModel fromModel(@Nonnull final com.azure.resourcemanager.cognitiveservices.models.AccountModel accountModel) {
        final List<ModelSku> skus = Optional.ofNullable(accountModel.skus())
            .map(s -> s.stream().map(ModelSku::fromModelSku).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
        return AccountModel.builder()
            .format(accountModel.format())
            .name(accountModel.name())
            .version(accountModel.version())
            .source(accountModel.source())
            .skus(skus)
            .build();
    }

    public boolean isGPTModel() {
        return StringUtils.startsWith(this.name, "gpt");
    }

    public boolean isCodeModel() {
        return StringUtils.startsWith(this.name, "code");
    }

    public boolean isTextModel() {
        return StringUtils.startsWith(this.name, "text");
    }
}
