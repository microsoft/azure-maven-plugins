/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.legacy.function.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BindingConfiguration {
    private String type;
    private String direction;

    public boolean isTrigger() {
        return StringUtils.equalsIgnoreCase(this.getDirection(), BindingEnum.Direction.IN.name()) &&
                StringUtils.containsIgnoreCase(this.getType(), "trigger");
    }
}
