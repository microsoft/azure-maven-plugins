/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.entity;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@SuperBuilder
public class FunctionEntity {
    private final String triggerId;
    private final String functionAppId;
    private final String name;
    private final String scriptFile;
    private final String entryPoint;
    private final String triggerUrl;
    private final List<BindingEntity> bindingList;

    @Nullable
    public BindingEntity getTrigger() {
        if (CollectionUtils.isEmpty(bindingList)) {
            return null;
        }
        return bindingList.stream()
                .filter(bindingResource -> StringUtils.equals(bindingResource.getDirection(), "in") &&
                        StringUtils.containsIgnoreCase(bindingResource.getType(), "trigger"))
                .findFirst().orElse(null);
    }

    @Getter
    @SuperBuilder(toBuilder = true)
    public static class BindingEntity {
        private final String type;
        private final String direction;
        private final String name;
        private final Map<String, String> properties;

        public String getProperty(String key) {
            return Optional.ofNullable(properties).map(map -> map.get(key)).orElse(null);
        }
    }
}
