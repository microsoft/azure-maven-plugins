/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.appcontainers.models.Container;
import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.azure.resourcemanager.appcontainers.models.RegistryCredentials;
import com.azure.resourcemanager.appcontainers.models.Secret;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ContainerAppDraft extends ContainerApp implements AzResource.Draft<ContainerApp, com.azure.resourcemanager.appcontainers.models.ContainerApp> {
    @Getter
    @Nullable
    private final ContainerApp origin;

    @Getter
    @Setter
    private Config config;

    protected ContainerAppDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ContainerAppModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    protected ContainerAppDraft(@Nonnull ContainerApp origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {

    }

    @Nonnull
    @Override
    public com.azure.resourcemanager.appcontainers.models.ContainerApp createResourceInAzure() {
        return null;
    }

    @Nonnull
    @Override
    public com.azure.resourcemanager.appcontainers.models.ContainerApp updateResourceInAzure(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp origin) {
        return this.updateImage(origin);
    }

    @Nonnull
    @AzureOperation(name = "azure/aca.deploy_image.app", params = {"this.getName()"})
    private com.azure.resourcemanager.appcontainers.models.ContainerApp updateImage(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp origin) {
        final ImageConfig config = this.getConfig().getImageConfig();
        final IAzureMessager messager = AzureMessager.getMessager();

        final String name = this.getName();
        messager.info(AzureString.format("Start updating image in Container App({0})...", name));
        final com.azure.resourcemanager.appcontainers.models.ContainerApp.Update update = origin.update();
        final ContainerRegistry registry = config.getContainerRegistry();
        if (Objects.nonNull(registry)) { // update registries and secrets for ACR
            final String username = registry.getUserName();
            final String password = Optional.ofNullable(registry.getPrimaryCredential()).orElseGet(registry::getSecondaryCredential);
            final String passwordKey = Objects.equals(password, registry.getPrimaryCredential()) ? "password" : "password2";
            final String passwordName = String.format("%s-%s", registry.getName().toLowerCase(), passwordKey);
            final List<Secret> secrets = origin.listSecrets().value().stream().map(s -> new Secret().withName(s.name()).withValue(s.value())).collect(Collectors.toList());
            final List<RegistryCredentials> registries = Optional.ofNullable(origin.configuration().registries()).map(ArrayList::new).orElseGet(ArrayList::new);
            registries.removeIf(r -> r.server().equalsIgnoreCase(registry.getLoginServerUrl()));
            registries.add(new RegistryCredentials().withServer(registry.getLoginServerUrl()).withUsername(username).withPasswordSecretRef(passwordName));
            secrets.removeIf(s -> s.name().equalsIgnoreCase(passwordName));
            secrets.add(new Secret().withName(passwordName).withValue(password));
            update.withConfiguration(origin.configuration()
                .withRegistries(registries)
                .withSecrets(secrets));
        }

        // update container/image
        final String imageId = config.getFullImageName();
        final String containerName = getContainerNameForImage(imageId);
        // drop old containers because we want to replace the old image
        final List<Container> containers = Collections.singletonList(new Container().withName(containerName).withImage(imageId).withEnv(config.getEnvironmentVariables()));
        update.withTemplate(origin.template().withContainers(containers));
        final com.azure.resourcemanager.appcontainers.models.ContainerApp updated = update.apply();
        messager.info(AzureString.format("Image in Container App({0}) is successfully updated.", name));
        return updated;
    }

    private static String getContainerNameForImage(String containerImageName) {
        return containerImageName.substring(containerImageName.lastIndexOf('/') + 1).replaceAll("[^0-9a-zA-Z-]", "-");
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Data
    public static class Config {
        public ImageConfig imageConfig;
    }

    @Setter
    @Getter
    public static class ImageConfig {
        @Nullable
        private ContainerRegistry containerRegistry;
        @Nonnull
        private String fullImageName;
        @Nonnull
        private List<EnvironmentVar> environmentVariables = new ArrayList<>();

        public String getSimpleImageName() {
            return fullImageName.substring(0, fullImageName.lastIndexOf(':')).substring(fullImageName.lastIndexOf('/') + 1);
        }

        public String getTag() {
            return Optional.of(fullImageName.substring(fullImageName.lastIndexOf(':') + 1)).filter(StringUtils::isNotBlank).orElse("latest");
        }
    }
}
