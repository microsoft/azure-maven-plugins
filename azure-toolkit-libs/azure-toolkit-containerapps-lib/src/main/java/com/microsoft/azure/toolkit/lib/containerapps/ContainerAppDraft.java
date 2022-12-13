/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps;

import com.azure.resourcemanager.appcontainers.fluent.models.ContainerAppInner;
import com.azure.resourcemanager.appcontainers.models.ActiveRevisionsMode;
import com.azure.resourcemanager.appcontainers.models.Container;
import com.azure.resourcemanager.appcontainers.models.ContainerApp;
import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.azure.resourcemanager.appcontainers.models.RegistryCredentials;
import com.azure.resourcemanager.appcontainers.models.Secret;
import com.azure.resourcemanager.appcontainers.models.Template;
import com.azure.resourcemanager.appcontainers.models.Volume;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.model.IContainerRegistry;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ContainerAppDraft {

    @Getter
    @Setter
    private Config config;

    @Nonnull
    @AzureOperation(name = "azure/aca.deploy_image.app", params = {"origin.name()"})
    public ContainerApp deployImage(@Nonnull ContainerApp origin) {
        final Config config = this.getConfig();
        final IAzureMessager messager = AzureMessager.getMessager();
        if (hasUnsupportedFeatures(origin)) { // move to intellij
            final ActiveRevisionsMode revisionsMode = origin.configuration().activeRevisionsMode();
            AzureString message = revisionsMode == ActiveRevisionsMode.SINGLE ?
                AzureString.format("Are you sure you want to deploy to \"%s\"? This will overwrite the active revision and unsupported features in VS Code will be lost.", origin.name()) :
                AzureString.format("Are you sure you want to deploy to \"%s\"? Unsupported features in VS Code will be lost in the new revision.", origin.name());
            if (!messager.confirm(message)) {
                return origin;
            }
        }

        final ContainerApp.Update update = origin.update();
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
        final String imageId = config.getImageId();
        final String containerName = getContainerNameForImage(imageId);
        // drop old containers because we want to replace the old image
        final List<Container> containers = Collections.singletonList(new Container().withName(containerName).withImage(imageId).withEnv(config.getEnvironmentVariables()));
        update.withTemplate(origin.template().withContainers(containers));
        update.apply();
        return origin;
    }

    // refer to https://github.com/microsoft/vscode-azurecontainerapps/main/src/commands/deployImage/deployImage.ts#L111
    public boolean hasUnsupportedFeatures(ContainerApp app) {
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

    @Data
    public static class Config {
        private IContainerRegistry containerRegistry;
        private String imageId;
        private List<EnvironmentVar> environmentVariables;
    }
}
