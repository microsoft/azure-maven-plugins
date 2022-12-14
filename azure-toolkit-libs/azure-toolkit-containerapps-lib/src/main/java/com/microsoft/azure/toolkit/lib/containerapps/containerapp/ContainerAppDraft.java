/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.appcontainers.fluent.models.ContainerAppInner;
import com.azure.resourcemanager.appcontainers.models.Container;
import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.azure.resourcemanager.appcontainers.models.RegistryCredentials;
import com.azure.resourcemanager.appcontainers.models.Secret;
import com.azure.resourcemanager.appcontainers.models.Template;
import com.azure.resourcemanager.appcontainers.models.Volume;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.model.IContainerRegistry;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
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
        final IContainerRegistry iRegistry = config.getContainerRegistry();
        if (Objects.nonNull(iRegistry) && (iRegistry instanceof ContainerRegistry)) { // update registries and secrets for ACR
            final ContainerRegistry registry = (ContainerRegistry) iRegistry;
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
        update.apply();
        messager.info(AzureString.format("Image in Container App({0}) is successfully updated.", name));
        return origin;
    }

    // refer to https://github.com/microsoft/vscode-azurecontainerapps/main/src/commands/deployImage/deployImage.ts#L111
    public boolean hasUnsupportedFeatures(com.azure.resourcemanager.appcontainers.models.ContainerApp app) {
        final Optional<Template> opTemplate = Optional.ofNullable(app.innerModel()).map(ContainerAppInner::template);
        final List<Container> containers = opTemplate.map(Template::containers).filter(CollectionUtils::isNotEmpty).orElse(null);
        final List<Volume> volumes = opTemplate.map(Template::volumes).orElse(null);
        if (CollectionUtils.isNotEmpty(volumes)) {
            return true;
        } else if (CollectionUtils.isNotEmpty(containers)) {
            if (containers.size() > 1) {
                return true;
            }
            for (Container container : containers) {
                // NOTE: these are all arrays so if they are empty, this will still return true
                // but these should be undefined if not being utilized
                return CollectionUtils.isNotEmpty(container.probes()) ||
                    CollectionUtils.isNotEmpty(container.volumeMounts()) ||
                    CollectionUtils.isNotEmpty(container.args());
            }
        }
        return false;
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

    @Data
    public static class ImageConfig {
        @Nullable
        private IContainerRegistry containerRegistry;
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
