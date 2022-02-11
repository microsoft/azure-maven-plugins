/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentMode;
import com.azure.resourcemanager.resources.models.DeploymentOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceDeployment extends AbstractAzResource<ResourceDeployment, ResourceGroup, Deployment> implements Removable {
    private static final String EMPTY_PARAMETER = "{}";
    private static final String[] VALID_PARAMETER_ATTRIBUTES = {"value", "reference", "metadata"};

    protected ResourceDeployment(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ResourceDeploymentModule module) {
        super(name, resourceGroupName, module);
    }

    /**
     * copy constructor
     */
    protected ResourceDeployment(@Nonnull ResourceDeployment origin) {
        super(origin);
    }

    protected ResourceDeployment(@Nonnull Deployment remote, @Nonnull ResourceDeploymentModule module) {
        super(remote.name(), remote.name(), module);
        this.setRemote(remote);
    }

    @Override
    public List<AzResourceModule<?, ResourceDeployment, ?>> getSubModules() {
        return Collections.emptyList();
    }

    public String getMode() {
        return this.remoteOptional().map(Deployment::mode).orElse(DeploymentMode.INCREMENTAL).toString();
    }

    public OffsetDateTime getTimestamp() {
        return this.remoteOptional().map(Deployment::timestamp).orElse(null);
    }

    public Stream<DeploymentOperation> getOperations() {
        return this.remoteOptional().map(d -> d.deploymentOperations().list().stream()).orElse(Stream.empty());
    }

    public List<String> getParameters() {
        return this.remoteOptional().map(r -> ((Map<String, Object>) r.exportTemplate().template()))
            .map(t -> ((Map<String, Map<String, String>>) t.get("parameters")))
            .map(m -> m.entrySet().stream()).orElseGet(Stream::empty)
            .map(p -> String.format("%s(%s)", p.getKey(), p.getValue().get("type")))
            .collect(Collectors.toList());
    }

    public List<String> getVariables() {
        return this.remoteOptional().map(r -> ((Map<String, Object>) r.exportTemplate().template()))
            .map(t -> ((Map<String, Map<String, String>>) t.get("variables")))
            .map(m -> m.keySet().stream()).orElseGet(Stream::empty)
            .collect(Collectors.toList());
    }

    public List<String> getResources() {
        return this.remoteOptional().map(r -> ((Map<String, Object>) r.exportTemplate().template()))
            .map(t -> ((List<Map<String, String>>) t.get("resources")))
            .map(Collection::stream).orElseGet(Stream::empty).map(r -> String.format("%s(%s)", r.get("name"), r.get("type")))
            .collect(Collectors.toList());
    }

    @Nullable
    public String getTemplateAsJson() {
        final ObjectMapper mapper = new ObjectMapper();
        return this.remoteOptional().map(r -> r.exportTemplate().template()).map(t -> {
            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(t);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).orElse(null);
    }

    @Nullable
    public String getParametersAsJson() {
        final ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings({"UnnecessaryParentheses", "unchecked"}) final Map<String, Map<String, String>> parameters = ((Map<String, Map<String, String>>) this.remoteOptional().map(Deployment::parameters)
            .filter(p -> p instanceof Map).orElse(null));
        // Remove extra attributes in parameters
        // Refers https://schema.management.azure.com/schemas/2015-01-01/deploymentParameters.json#
        if (Objects.nonNull(parameters)) {
            try {
                parameters.values().forEach(value -> {
                    final Iterator<Map.Entry<String, String>> iterator = value.entrySet().iterator();
                    while (iterator.hasNext()) {
                        final String parameterKey = iterator.next().getKey();
                        if (Arrays.stream(VALID_PARAMETER_ATTRIBUTES).noneMatch(attribute -> attribute.equals(parameterKey))) {
                            iterator.remove();
                        }
                    }
                });
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters);
            } catch (JsonProcessingException var2) {
                return EMPTY_PARAMETER;
            }
        }
        return EMPTY_PARAMETER;
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull Deployment remote) {
        return remote.provisioningState();
    }

    @Override
    public String status() {
        return this.getStatus();
    }

    @Override
    public void remove() {
        this.delete();
    }
}
