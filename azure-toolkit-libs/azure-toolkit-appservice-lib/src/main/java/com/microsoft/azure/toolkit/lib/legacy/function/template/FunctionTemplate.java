/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionTemplate {
    private String id;
    private TemplateMetadata metadata;
    private List<String> bundle;
    private Function function;
    private Map<String, String> files;

    @JsonIgnore
    @Getter
    @Setter
    private BindingTemplate binding;

    public Set<FunctionExtensionVersion> getSupportedExtensionVersions() {
        return CollectionUtils.isEmpty(this.bundle) ? null : bundle.stream().map(FunctionUtils::parseFunctionExtensionVersion).collect(Collectors.toSet());
    }

    @Nullable
    public BindingConfiguration getBindingConfiguration() {
        return isTriggerTemplate() ? getTrigger() : Optional.ofNullable(function)
                .map(Function::getBindings).filter(CollectionUtils::isNotEmpty).map(bindings -> bindings.get(0)).orElse(null);
    }

    public boolean isTriggerTemplate() {
        return Objects.nonNull(getTrigger());
    }

    @Nullable
    public BindingConfiguration getTrigger() {
        final List<BindingConfiguration> bindings = Optional.ofNullable(function).map(Function::getBindings).orElse(Collections.emptyList());
        return bindings.stream().filter(BindingConfiguration::isTrigger).findFirst().orElse(null);
    }

    @Nullable
    @Deprecated
    public String getTriggerType() {
        return Optional.ofNullable(getBindingConfiguration()).map(BindingConfiguration::getType).orElse(null);
    }

    public String generateContent(final Map<String, String> parameters) {
        String templateContent = Objects.requireNonNull(getFiles().get("function.java"), String.format("failed to load template of binding %s", getFunction()));
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            templateContent = templateContent.replace(String.format("$%s$", entry.getKey()), entry.getValue());
        }
        return templateContent;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Function {
        private boolean disabled;
        private List<BindingConfiguration> bindings;
    }
}
