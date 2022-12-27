/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.appcontainers.implementation.ContainerAppImpl;
import com.azure.resourcemanager.appcontainers.models.ActiveRevisionsMode;
import com.azure.resourcemanager.appcontainers.models.Configuration;
import com.azure.resourcemanager.appcontainers.models.Container;
import com.azure.resourcemanager.appcontainers.models.ContainerApps;
import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.azure.resourcemanager.appcontainers.models.RegistryCredentials;
import com.azure.resourcemanager.appcontainers.models.Secret;
import com.azure.resourcemanager.appcontainers.models.Template;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.containerapps.model.RevisionMode;
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
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/containerapps.create_app.app", params = {"this.getName()"})
    public com.azure.resourcemanager.appcontainers.models.ContainerApp createResourceInAzure() {
        final ContainerApps client = Objects.requireNonNull(((ContainerAppModule) getModule()).getClient());
        final ImageConfig imageConfig = Objects.requireNonNull(ensureConfig().getImageConfig(), "Image is required to create Container app.");
        final Configuration configuration = new Configuration();
        Optional.ofNullable(ensureConfig().getRevisionMode()).ifPresent(mode ->
            configuration.withActiveRevisionsMode(ActiveRevisionsMode.fromString(ensureConfig().getRevisionMode().getValue())));
        configuration.withSecrets(Collections.singletonList(getSecret(imageConfig)));
        configuration.withRegistries(Collections.singletonList(getRegistryCredential(imageConfig)));
        configuration.withIngress(Optional.ofNullable(ensureConfig().getIngressConfig()).map(IngressConfig::toIngress).orElse(null));
        final Template template = new Template().withContainers(getContainers(imageConfig));
        AzureMessager.getMessager().info(AzureString.format("Start creating Azure Container App({0})...", this.getName()));
        final com.azure.resourcemanager.appcontainers.models.ContainerApp result = client.define(ensureConfig().getName())
            .withRegion(com.azure.core.management.Region.fromName(ensureConfig().getRegion().getName()))
            .withExistingResourceGroup(ensureConfig().getResourceGroupName())
            .withManagedEnvironmentId(Objects.requireNonNull(ensureConfig().getEnvironment(), "Environment is required to create Container app.").getId())
            .withConfiguration(configuration)
            .withTemplate(template)
            .create();
        AzureMessager.getMessager().success(AzureString.format("Azure Container App({0}) is successfully created.", this.getName()));
        return result;
    }

    @Nullable
    public IngressConfig getIngressConfig() {
        return Optional.ofNullable(config).map(Config::getIngressConfig).orElse(super.getIngressConfig());
    }

    @Nullable
    public RevisionMode getRevisionMode() {
        return Optional.ofNullable(config).map(Config::getRevisionMode).orElse(super.getRevisionMode());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/containerapps.update_app.app", params = {"this.getName()"})
    public com.azure.resourcemanager.appcontainers.models.ContainerApp updateResourceInAzure(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp origin) {
        final IAzureMessager messager = AzureMessager.getMessager();
        final Config config = ensureConfig();
        final ImageConfig imageConfig = config.getImageConfig();
        final IngressConfig ingressConfig = config.getIngressConfig();
        final RevisionMode revisionMode = config.getRevisionMode();

        final boolean isImageModified = Objects.nonNull(imageConfig);
        final boolean isIngressConfigModified = Objects.nonNull(ingressConfig) && !Objects.equals(ingressConfig, super.getIngressConfig());
        final boolean isRevisionModeModified = !Objects.equals(revisionMode, super.getRevisionMode());
        final boolean isModified = isImageModified || isIngressConfigModified || isRevisionModeModified;
        if (!isModified) {
            return origin;
        }
        final ContainerAppImpl update =
            (ContainerAppImpl) (isImageModified ? this.updateImage(origin) : origin.update());
        final Configuration configuration = update.configuration();
        if (!isImageModified) {
            // clear registries & secrets configuration if image is not updated
            configuration.withRegistries(null).withSecrets(null);
        }
        if (isIngressConfigModified) {
            configuration.withIngress(ingressConfig.toIngress());
        }
        if (isRevisionModeModified) {
            configuration.withActiveRevisionsMode(revisionMode.toActiveRevisionMode());
        }
        update.withConfiguration(configuration);
        messager.info(AzureString.format("Start updating Container App({0})...", getName()));
        final com.azure.resourcemanager.appcontainers.models.ContainerApp result = update.apply();
        messager.info(AzureString.format("Container App({0}) is successfully updated.", getName()));
        if (isImageModified) {
            AzureTaskManager.getInstance().runOnPooledThread(() -> this.getRevisionModule().refresh());
        }
        return result;
    }

    @Nonnull
    private com.azure.resourcemanager.appcontainers.models.ContainerApp.Update updateImage(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp origin) {
        final ImageConfig config = Objects.requireNonNull(this.getConfig().getImageConfig(), "image config is null.");
        final com.azure.resourcemanager.appcontainers.models.ContainerApp.Update update = origin.update();
        final ContainerRegistry registry = config.getContainerRegistry();
        final List<Secret> secrets = origin.listSecrets().value().stream().map(s -> new Secret().withName(s.name()).withValue(s.value())).collect(Collectors.toList());
        final List<RegistryCredentials> registries = Optional.ofNullable(origin.configuration().registries()).map(ArrayList::new).orElseGet(ArrayList::new);
        if (Objects.nonNull(registry)) { // update registries and secrets for ACR
            Optional.ofNullable(getSecret(config)).ifPresent(secret -> {
                secrets.removeIf(r -> r.name().equalsIgnoreCase(secret.name()));
                secrets.add(secret);
            });
            Optional.ofNullable(getRegistryCredential(config)).ifPresent(credential -> {
                registries.removeIf(r -> r.server().equalsIgnoreCase(credential.server()));
                registries.add(credential);
            });
        }
        update.withConfiguration(origin.configuration()
            .withRegistries(registries)
            .withSecrets(secrets));
        // drop old containers because we want to replace the old image
        return update.withTemplate(origin.template().withContainers(getContainers(config)));
    }

    private static Secret getSecret(final ImageConfig config) {
        final ContainerRegistry registry = config.getContainerRegistry();
        if (Objects.nonNull(registry)) {
            final String password = Optional.ofNullable(registry.getPrimaryCredential()).orElseGet(registry::getSecondaryCredential);
            final String passwordKey = Objects.equals(password, registry.getPrimaryCredential()) ? "password" : "password2";
            final String passwordName = String.format("%s-%s", registry.getName().toLowerCase(), passwordKey);
            return new Secret().withName(passwordName).withValue(password);
        }
        return null;
    }

    private static RegistryCredentials getRegistryCredential(final ImageConfig config) {
        final ContainerRegistry registry = config.getContainerRegistry();
        if (Objects.nonNull(registry)) {
            final String username = registry.getUserName();
            final String password = Optional.ofNullable(registry.getPrimaryCredential()).orElseGet(registry::getSecondaryCredential);
            final String passwordKey = Objects.equals(password, registry.getPrimaryCredential()) ? "password" : "password2";
            final String passwordName = String.format("%s-%s", registry.getName().toLowerCase(), passwordKey);
            return new RegistryCredentials().withServer(registry.getLoginServerUrl()).withUsername(username).withPasswordSecretRef(passwordName);
        }
        return null;
    }

    private static List<Container> getContainers(@Nonnull final ImageConfig config) {
        final String imageId = config.getFullImageName();
        final String containerName = getContainerNameForImage(imageId);
        // drop old containers because we want to replace the old image
        return Collections.singletonList(new Container().withName(containerName).withImage(imageId).withEnv(config.getEnvironmentVariables()));
    }

    private static String getContainerNameForImage(String containerImageName) {
        return containerImageName.substring(containerImageName.lastIndexOf('/') + 1).replaceAll("[^0-9a-zA-Z-]", "-");
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    @Override
    public boolean isModified() {
        return this.config == null || Objects.equals(this.config, new Config());
    }

    @Data
    public static class Config {
        private String name;
        private String resourceGroupName;
        private Region region;
        @Nullable
        private ContainerAppsEnvironment environment;
        private RevisionMode revisionMode = RevisionMode.SINGLE;
        @Nullable
        private ImageConfig imageConfig;
        @Nullable
        private IngressConfig ingressConfig;
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
