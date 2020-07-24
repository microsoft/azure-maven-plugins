/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.function.model;

import com.microsoft.azure.common.Utils;
import com.microsoft.azure.management.appservice.FunctionEnvelope;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FunctionResource {
    private static final String FUNCTIONS = "functions";
    private static final String SCRIPT_FILE = "scriptFile";
    private static final String ENTRY_POINT = "entryPoint";
    private static final String BINDINGS = "bindings";

    private String triggerId;
    private String functionAppId;
    private String name;
    private String scriptFile;
    private String entryPoint;
    private String triggerUrl;
    private List<BindingResource> bindingList;

    public static FunctionResource parseFunction(FunctionEnvelope functionEnvelope) {
        final FunctionResource resource = new FunctionResource();
        resource.triggerId = functionEnvelope.inner().id();
        resource.name = Utils.getSegment(resource.triggerId, FUNCTIONS);
        resource.functionAppId = functionEnvelope.functionAppId();
        resource.triggerUrl = functionEnvelope.inner().invokeUrlTemplate();
        final Object config = functionEnvelope.config();
        if (config instanceof Map) {
            resource.scriptFile = (String) ((Map) config).get(SCRIPT_FILE);
            resource.entryPoint = (String) ((Map) config).get(ENTRY_POINT);
            Object bindingListObject = ((Map) config).get(BINDINGS);
            if (bindingListObject instanceof List) {
                resource.bindingList = (List<BindingResource>) ((List) bindingListObject).stream()
                        .filter(item -> item instanceof Map)
                        .map(map -> BindingResource.parseBinding((Map) map))
                        .collect(Collectors.toList());
            }
        }
        return resource;
    }

    public String getName() {
        return name;
    }

    public String getTriggerId() {
        return triggerId;
    }

    public String getFunctionAppId() {
        return functionAppId;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public List<BindingResource> getBindingList() {
        return bindingList;
    }

    public String getTriggerUrl() {
        return triggerUrl;
    }

    public BindingResource getTrigger() {
        return CollectionUtils.isEmpty(bindingList) ? null :
                bindingList.stream()
                        .filter(bindingResource -> StringUtils.equals(bindingResource.direction, "in") &&
                                StringUtils.containsIgnoreCase(bindingResource.getType(), "trigger"))
                        .findFirst().orElse(null);
    }

    public static class BindingResource {
        String type;
        String direction;
        String name;
        Map<String, ?> properties;

        public static BindingResource parseBinding(Map bindingMap) {
            final BindingResource resource = new BindingResource();
            resource.type = (String) bindingMap.get("type");
            resource.direction = (String) bindingMap.get("direction");
            resource.name = (String) bindingMap.get("name");
            resource.properties = bindingMap;
            return resource;
        }

        public String getType() {
            return type;
        }

        public String getDirection() {
            return direction;
        }

        public String getName() {
            return name;
        }

        public Map<String, ?> getProperties() {
            return properties;
        }

        public Object getProperty(String key) {
            return properties == null ? null : properties.get(key);
        }
    }
}
