/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;

// This is the json template class correspond to bindings.json
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BindingsTemplate {
    @JsonProperty("$schema")
    private String schema;
    private String contentVersion;
    private Map<String, String> variables;
    private BindingTemplate[] bindings;

    public BindingTemplate getBindingTemplate(@Nonnull final BindingConfiguration conf) {
        return Arrays.stream(this.bindings)
                .filter(binding -> StringUtils.equalsIgnoreCase(binding.getType(), conf.getType()) &&
                        (conf.isTrigger() && StringUtils.equalsIgnoreCase(binding.getDirection(), "trigger") ||
                                StringUtils.equalsIgnoreCase(binding.getDirection(), conf.getDirection())))
                .findFirst()
                .orElse(null);
    }
}
