/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.core.management.serializer.SerializerFactory;
import com.azure.core.util.serializer.SerializerAdapter;
import com.azure.core.util.serializer.SerializerEncoding;
import com.azure.resourcemanager.appcontainers.implementation.ContainerAppImpl;
import com.azure.resourcemanager.appcontainers.models.ActiveRevisionsMode;
import com.azure.resourcemanager.appcontainers.models.Configuration;
import com.azure.resourcemanager.appcontainers.models.Container;
import com.azure.resourcemanager.appcontainers.models.ContainerApps;
import com.azure.resourcemanager.appcontainers.models.ContainerResources;
import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.azure.resourcemanager.appcontainers.models.ManagedServiceIdentity;
import com.azure.resourcemanager.appcontainers.models.ManagedServiceIdentityType;
import com.azure.resourcemanager.appcontainers.models.RegistryCredentials;
import com.azure.resourcemanager.appcontainers.models.Runtime;
import com.azure.resourcemanager.appcontainers.models.RuntimeJava;
import com.azure.resourcemanager.appcontainers.models.Scale;
import com.azure.resourcemanager.appcontainers.models.Secret;
import com.azure.resourcemanager.appcontainers.models.Template;
import com.azure.resourcemanager.appcontainers.models.UserAssignedIdentity;
import com.azure.resourcemanager.authorization.AuthorizationManager;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.azure.resourcemanager.containerregistry.models.OverridingArgument;
import com.azure.resourcemanager.containerregistry.models.RegistryTaskRun;
import com.google.common.collect.Sets;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.EnvironmentType;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.containerapps.model.ResourceConfiguration;
import com.microsoft.azure.toolkit.lib.containerapps.model.RevisionMode;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfile;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistryModule;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistryDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.model.Sku;
import com.microsoft.azure.toolkit.lib.identities.AzureManagedIdentity;
import com.microsoft.azure.toolkit.lib.identities.Identity;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry.ACR_IMAGE_SUFFIX;

public class ContainerAppDraft extends ContainerApp implements AzResource.Draft<ContainerApp, com.azure.resourcemanager.appcontainers.models.ContainerApp> {
    private static final String sourceDockerFilePath = "template/aca/source-dockerfile";
    private static final String artifactDockerFilePath = "template/aca/artifact-dockerfile";
    public static final String ACR_PULL_ROLE_ID = "7f951dda-4ed3-4680-a7ca-43fe172d538d";

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

        final ContainerAppsEnvironment containerAppsEnvironment = Objects.requireNonNull(ensureConfig().getEnvironment(),
            "Environment is required to create Container app.");
        if (containerAppsEnvironment.isDraftForCreating()) {
            ((ContainerAppsEnvironmentDraft) containerAppsEnvironment).commit();
        }
        final ImageConfig imageConfig = Objects.requireNonNull(this.getImageConfig(), "Image is required to create Container app.");
        buildImageIfNeeded(imageConfig);
        final Configuration configuration = new Configuration();
        Optional.ofNullable(ensureConfig().getRevisionMode()).ifPresent(mode ->
            configuration.withActiveRevisionsMode(ActiveRevisionsMode.fromString(ensureConfig().getRevisionMode().getValue())));
        configuration.withSecrets(Optional.ofNullable(getSecret(imageConfig)).map(Collections::singletonList).orElse(Collections.emptyList()));
        configuration.withRegistries(Optional.ofNullable(getRegistryCredential(imageConfig)).map(Collections::singletonList).orElse(Collections.emptyList()));
        configuration.withIngress(Optional.ofNullable(ensureConfig().getIngressConfig()).map(IngressConfig::toIngress).orElse(null));
        configuration.withRuntime(new Runtime().withJava(new RuntimeJava().withEnableMetrics(true)));
        final ResourceConfiguration resourceConfiguration = ensureConfig().getResourceConfiguration();
        final Template template = new Template()
            .withContainers(ImageConfig.toContainers(imageConfig, resourceConfiguration))
            .withScale(ScaleConfig.toScale(this.getScaleConfig()));
        AzureMessager.getMessager().progress(AzureString.format("Creating Azure Container App({0})...", this.getName()));
        final String workloadProfile = containerAppsEnvironment.getEnvironmentType() == EnvironmentType.ConsumptionOnly ? null :
            Optional.ofNullable(getResourceConfiguration()).map(ResourceConfiguration::getWorkloadProfile).map(WorkloadProfile::getName).orElse(WorkloadProfile.CONSUMPTION);
        final com.azure.resourcemanager.appcontainers.models.ContainerApp result = client.define(ensureConfig().getName())
            .withRegion(com.azure.core.management.Region.fromName(containerAppsEnvironment.getRegion().getName()))
            .withExistingResourceGroup(Objects.requireNonNull(ensureConfig().getResourceGroup(), "Resource Group is required to create Container app.").getResourceGroupName())
            .withManagedEnvironmentId(containerAppsEnvironment.getId())
            .withConfiguration(configuration)
            .withTemplate(template)
            .withWorkloadProfileName(workloadProfile)
            .withIdentity(ensureMIAndACRPermission(imageConfig))
            .create();
        final Action<ContainerApp> updateImage = Optional.ofNullable(AzureActionManager.getInstance().getAction(ContainerApp.UPDATE_IMAGE))
            .map(action -> action.bind(this))
            .orElse(null);
        final Action<ContainerApp> browse = Optional.ofNullable(AzureActionManager.getInstance().getAction(ContainerApp.BROWSE))
            .map(action -> action.bind(this))
            .orElse(null);

        AzureMessager.getMessager().success(AzureString.format("Azure Container App({0}) is successfully created.", this.getName()), browse, updateImage);
        printSuccessMessages();
        return result;
    }

    // todo: support update workload profile properties
    @Nonnull
    @Override
    @AzureOperation(name = "azure/containerapps.update_app.app", params = {"this.getName()"})
    public com.azure.resourcemanager.appcontainers.models.ContainerApp updateResourceInAzure(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp origin) {
        final IAzureMessager messager = AzureMessager.getMessager();
        final Config config = ensureConfig();
        final ImageConfig imageConfig = config.getImageConfig();
        final IngressConfig ingressConfig = config.getIngressConfig();
        final RevisionMode revisionMode = config.getRevisionMode();
        final ScaleConfig scaleConfig = config.getScaleConfig();

        final boolean isImageModified = Objects.nonNull(imageConfig) && !Objects.equals(imageConfig, super.getImageConfig());
        final boolean isIngressConfigModified = Objects.nonNull(ingressConfig) && !Objects.equals(ingressConfig, super.getIngressConfig());
        final boolean isRevisionModeModified = !Objects.equals(revisionMode, super.getRevisionMode());
        final boolean isScaleModified = !Objects.equals(scaleConfig, super.getScaleConfig());
        final boolean isModified = isImageModified || isIngressConfigModified || isRevisionModeModified || isScaleModified;
        if (!isModified) {
            return origin;
        }
        buildImageIfNeeded(imageConfig);
        final ContainerAppImpl update = (ContainerAppImpl) (isImageModified ? this.updateImage(origin) : origin.update());
        final Configuration configuration = update.configuration();
        if (!isImageModified) {
            // anytime you want to update the container app, you need to include the secrets but that is not retrieved by default
            final List<Secret> secrets = origin.listSecrets().value().stream().map(s -> new Secret().withName(s.name()).withValue(s.value())).collect(Collectors.toList());
            final List<RegistryCredentials> registries = Optional.ofNullable(origin.configuration().registries()).map(ArrayList::new).orElseGet(ArrayList::new);
            configuration.withRegistries(registries).withSecrets(secrets);
        }
        // ["properties"]["template"]["containers"]
        if (isIngressConfigModified) {
            configuration.withIngress(ingressConfig.toIngress());
        }
        if (isRevisionModeModified) {
            configuration.withActiveRevisionsMode(revisionMode.toActiveRevisionMode());
        }
        if (isScaleModified) {
            if (isImageModified) {
                update.withTemplate(update.template().withScale(ScaleConfig.toScale(scaleConfig)));
            } else {
                update.withTemplate(new Template().withScale(ScaleConfig.toScale(scaleConfig)));
            }
        }
        update.withConfiguration(configuration);
        ManagedServiceIdentity identity = ensureMIAndACRPermission(imageConfig);
        if (Objects.nonNull(identity)) {
            update.withIdentity(identity);
        }
        messager.progress(AzureString.format("Updating Container App({0})...", getName()));
        final com.azure.resourcemanager.appcontainers.models.ContainerApp result = update.apply();
        final Action<ContainerApp> browse = Optional.ofNullable(AzureActionManager.getInstance().getAction(ContainerApp.BROWSE))
            .map(action -> action.bind(this))
            .orElse(null);
        messager.success(AzureString.format("Container App({0}) is successfully updated.", getName()), browse);
        printSuccessMessages();
        if (isImageModified) {
            AzureTaskManager.getInstance().runOnPooledThread(() -> this.getRevisionModule().refresh());
        }
        return result;
    }

    private void printSuccessMessages() {
        final Action<String> learnMore = Optional.ofNullable(AzureActionManager.getInstance().getAction(Action.OPEN_URL).withLabel("Learn More"))
            .map(action -> action.bind("https://aka.ms/azuretools-aca-stack"))
            .orElse(null);
        final Action<String> openPortal = Optional.ofNullable(AzureActionManager.getInstance().getAction(Action.OPEN_URL).withLabel("Open Portal"))
            .map(action -> action.bind(this.getPortalUrl()))
            .orElse(null);
        final Action<String> openApp = Optional.ofNullable(this.getIngressFqdn())
            .filter(fqdn -> !StringUtils.isEmpty(fqdn))
            .flatMap(fqdn -> Optional.ofNullable(AzureActionManager.getInstance()
                    .getAction(Action.OPEN_URL)
                    .withLabel("Open Application"))
                .map(action -> action.bind(String.format("https://%s", fqdn))))
            .orElse(null);

        AzureMessager.getMessager().info("To take advantage of the Java-optimized feature, please set your development stack to `Java` in the portal.", learnMore, openPortal, openApp);
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
        return update.withTemplate(origin.template().withContainers(ImageConfig.toContainers(config)));
    }

    public void buildImageIfNeeded(ImageConfig imageConfig) {
        if (!Optional.ofNullable(imageConfig).map(ImageConfig::getBuildImageConfig).map(b -> b.source).filter(Files::exists).isPresent()) {
            OperationContext.action().setTelemetryProperty("needBuildImage", "false");
            return;
        }
        OperationContext.action().setTelemetryProperty("needBuildImage", "true");
        OperationContext.action().setTelemetryProperty("hasDockerFile", String.valueOf(imageConfig.sourceHasDockerFile()));
        final BuildImageConfig buildConfig = Objects.requireNonNull(imageConfig.getBuildImageConfig());
        final String fullImageName;
        Path tempFolder = null;
        if (imageConfig.sourceHasDockerFile()) {
            // ACR Task is the only way we have for now to build a Dockerfile using Docker.
            AzureMessager.getMessager().warning("Dockerfile detected. Running the build through ACR.");
            fullImageName = buildThroughACR(imageConfig, buildConfig);
        } else {
            OperationContext.action().setTelemetryProperty("isDirectory", String.valueOf(Files.isDirectory(buildConfig.source)));
            if (Files.isDirectory(buildConfig.source)) {
                AzureMessager.getMessager().warning("No Dockerfile detected. Running the build through ACR with a generated Dockerfile.");
                generateDockerfile(buildConfig, sourceDockerFilePath);
            } else {
                AzureMessager.getMessager().warning("Building container image from artifact through ACR with a generated Dockerfile.");
                tempFolder = generateTempFolder(buildConfig);
            }
            fullImageName = buildThroughACR(imageConfig, buildConfig);
        }
        deleteTempFolder(tempFolder);
        if (StringUtils.isNotBlank(fullImageName)) {
            imageConfig.setFullImageName(fullImageName);
        }

    }

    private static void tarSourceIfNeeded(final BuildImageConfig buildConfig) {
        if (Files.isDirectory(buildConfig.source)) {
            final HashSet<String> ignored = Sets.newHashSet(".git", ".gitignore", ".bzr", "bzrignore", ".hg", ".hgignore", ".svn");
            AzureMessager.getMessager().progress(AzureString.format("Creating tar.gz from %s.", buildConfig.source.getFileName()));
            final Path sourceTar = Utils.tar(buildConfig.source, (path) -> ignored.contains(path.getFileName().toString()));
            buildConfig.setSource(sourceTar);
        }
    }

    private String buildThroughACR(final ImageConfig imageConfig, final BuildImageConfig buildConfig) {
        Map<String, OverridingArgument> overridingArguments = Optional.ofNullable(imageConfig.getBuildImageConfig())
            .map(BuildImageConfig::getSourceBuildEnv)
            .orElse(Collections.emptyMap())
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new OverridingArgument(e.getValue(), false)));
        final ContainerRegistry registry = getOrCreateRegistry(imageConfig);
        tarSourceIfNeeded(buildConfig);
        final RegistryTaskRun run = registry.buildImage(imageConfig.getAcrImageNameWithTag(), buildConfig.getSource(), "./Dockerfile", overridingArguments);
        if (Objects.isNull(run)) {
            throw new AzureToolkitRuntimeException("ACR is not ready, Failed to build image through ACR.");
        }
        return registry.waitForImageBuilding(run);
    }

    private static void deleteTempFolder(Path tempFolder) {
        if (Objects.isNull(tempFolder)) {
            return;
        }
        try {
            FileUtils.deleteDirectory(tempFolder.toFile());
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException("Failed to delete temporary directory: " + tempFolder, e);
        }
    }

    private static void generateDockerfile(final BuildImageConfig buildConfig, String templatePath) {
        Path destination = buildConfig.source.resolve("Dockerfile");
        // Path to the Dockerfile inside the resources/template folder
        // Load the Dockerfile from the resources
        InputStream inputStream = ContainerAppDraft.class.getClassLoader().getResourceAsStream(templatePath);
        if (inputStream == null) {
            throw new AzureToolkitRuntimeException("Template dockerfile not found in the resources: " + templatePath);
        }
        // Copy the Dockerfile to the destination path
        try {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException("Failed to copy Dockerfile to the destination path: " + destination, e);
        }
        AzureMessager.getMessager().info("Dockerfile generated successfully to: " + destination);
    }

    private static Path generateTempFolder(final BuildImageConfig buildConfig) {
        // Create a temporary directory and handle resources
        Path tempDir = null;
        try {
            // Step 1: Create a temporary directory
            tempDir = Files.createTempDirectory(String.format("aca-maven-plugin-%s", Utils.getTimestamp()));
            AzureMessager.getMessager().info("Temporary directory created: " + tempDir);

            // Step 2: Copy Jar to the temporary directory
            Path sourceFile = buildConfig.getSource();  // replace with your file path
            Path targetFile = tempDir.resolve("app.jar");
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            AzureMessager.getMessager().info("File copied to temporary directory: " + targetFile);

            buildConfig.setSource(tempDir);

            generateDockerfile(buildConfig, artifactDockerFilePath);

        } catch (IOException e) {
            if (Objects.nonNull(tempDir)) {
                deleteTempFolder(tempDir);
            }
            throw new AzureToolkitRuntimeException("Failed to create temporary directory and copy artifact", e);
        }
        return tempDir;
    }

    @Nonnull
    private ContainerRegistry getOrCreateRegistry(final ImageConfig config) {
        ContainerRegistry registry = config.getContainerRegistry();
        if (Objects.isNull(registry)) {
            final String registryName = Objects.requireNonNull(config.getAcrRegistryName());
            final AzureContainerRegistryModule registryModule = Azure.az(AzureContainerRegistry.class)
                .registry(this.getSubscriptionId());
            registry = registryModule.get(registryName, this.getResourceGroupName());
            if (Objects.isNull(registry)) {
                final List<ContainerRegistry> registries = registryModule.listByResourceGroup(this.getResourceGroupName());
                if (!registries.isEmpty()) {
                    registry = registries.stream().filter(ContainerRegistry::isAdminUserEnabled).findAny().orElse(null);
                    if (Objects.isNull(registry)) {
                        registry = registries.stream().findFirst().orElse(null);
                    }
                }
                if (Objects.isNull(registry)) {
                    AzureMessager.getMessager().info(AzureString.format("creating new container registry %s with admin user enabled.", registryName));
                    registry = registryModule.create(registryName, this.getResourceGroupName());
                    final ContainerRegistryDraft draft = (ContainerRegistryDraft) registry;
                    draft.setSku(Sku.Standard);
                    draft.setAdminUserEnabled(true);
                    draft.setRegion(Optional.ofNullable(this.getRegion()).orElse(Region.US_EAST));
                    draft.commit();
                } else {
                    AzureMessager.getMessager().info(AzureString.format("use container registry %s.", registry.getName()));
                }
            }
        }
        if (registry.isDraftForCreating()) {
            ((ContainerRegistryDraft) registry).setAdminUserEnabled(true);
            ((ContainerRegistryDraft) registry).commit();
        } else if (!registry.isAdminUserEnabled()) {// enable admin user
            AzureMessager.getMessager().info(AzureString.format("Enabling admin user for container registry %s.", registry.getName()));
            registry.enableAdminUser();
        }
        config.setContainerRegistry(registry);
        return registry;
    }

    @Nullable
    private static Secret getSecret(final ImageConfig config) {
        final ContainerRegistry registry = config.getContainerRegistry();
        if (Objects.nonNull(registry)) {
            if (StringUtils.isEmpty(config.identity)) {
                final String password = Optional.ofNullable(registry.getPrimaryCredential()).orElseGet(registry::getSecondaryCredential);
                final String passwordKey = Objects.equals(password, registry.getPrimaryCredential()) ? "password" : "password2";
                final String passwordName = String.format("%s-%s", registry.getName().toLowerCase(), passwordKey);
                return new Secret().withName(passwordName).withValue(password);
            }
        }
        return null;
    }

    @Nullable
    private static RegistryCredentials getRegistryCredential(final ImageConfig config) {
        final ContainerRegistry registry = config.getContainerRegistry();
        if (Objects.nonNull(registry)) {
            if (StringUtils.isEmpty(config.identity)) {
                final String username = registry.getUserName();
                final String password = Optional.ofNullable(registry.getPrimaryCredential()).orElseGet(registry::getSecondaryCredential);
                final String passwordKey = Objects.equals(password, registry.getPrimaryCredential()) ? "password" : "password2";
                final String passwordName = String.format("%s-%s", registry.getName().toLowerCase(), passwordKey);
                return new RegistryCredentials().withServer(registry.getLoginServerUrl()).withUsername(username).withPasswordSecretRef(passwordName);
            } else if (StringUtils.equalsIgnoreCase(config.identity, "system")) {
                return new RegistryCredentials().withServer(registry.getLoginServerUrl()).withIdentity("system");
            } else {
                return new RegistryCredentials().withServer(registry.getLoginServerUrl()).withIdentity(config.identity);
            }
        }
        return null;
    }

    // Only user assigned identity will be returned, and it will be added to the container app.
    // System assigned identity should be enabled before using it to pull acr image. So no need to return it here.
    @Nullable
    private ManagedServiceIdentity ensureMIAndACRPermission(ImageConfig imageConfig) {
        if (StringUtils.isBlank(imageConfig.getIdentity())) {
            return null;
        }
        if (StringUtils.equalsIgnoreCase(imageConfig.getIdentity(), "system")) {
            String principalId = Optional.ofNullable(this.origin)
                .map(ContainerApp::getIdentity)
                .filter(identity -> identity.type().equals(ManagedServiceIdentityType.SYSTEM_ASSIGNED) || identity.type().equals(ManagedServiceIdentityType.SYSTEM_ASSIGNED_USER_ASSIGNED))
                .map(identity -> identity.principalId().toString())
                .orElseThrow(() -> new AzureToolkitRuntimeException("System managed identity should be enabled before using to pull acr image."));
            grantACRPullPermissionToIdentity(imageConfig, principalId);
            return null;
        }
        try {
            Identity identity = Azure.az(AzureManagedIdentity.class).getById(imageConfig.getIdentity());
            grantACRPullPermissionToIdentity(imageConfig, identity.getPrincipalId());
            return new ManagedServiceIdentity().withType(ManagedServiceIdentityType.USER_ASSIGNED).withUserAssignedIdentities(Collections.singletonMap(identity.getId(), new UserAssignedIdentity()));
        } catch (Exception e) {
            throw new AzureToolkitRuntimeException("Failed to get Registry Identity.", e);
        }

    }

    private void grantACRPullPermissionToIdentity(ImageConfig imageConfig, String identityPrincipalId) {
        final String scope = imageConfig.getContainerRegistry().getId();
        final RoleAssignment existingAssignment = getExistingRoleAssignment(identityPrincipalId, scope);
        final String roleDefinitionId = String.format("/subscriptions/%s/providers/Microsoft.Authorization/roleDefinitions/%s", getSubscriptionId(), ACR_PULL_ROLE_ID);
        if (Objects.nonNull(existingAssignment)) {
            AzureMessager.getMessager().info("ACR pull permission already granted to the identity.");
            return;
        }
        final AuthorizationManager authorizationManager = this.getAuthorizationManager();
        final String roleAssignmentName = UUID.randomUUID().toString();
        authorizationManager.roleAssignments().define(roleAssignmentName)
            .forObjectId(identityPrincipalId)
            .withRoleDefinition(roleDefinitionId)
            .withScope(scope).create();
        AzureMessager.getMessager().info("ACR pull permission granted to the identity.");
    }

    private RoleAssignment getExistingRoleAssignment(final String identityId, final String scope) {
        final AuthorizationManager authorizationManager = this.getAuthorizationManager();
        final String roleDefinitionId = String.format("/subscriptions/%s/providers/Microsoft.Authorization/roleDefinitions/%s", getSubscriptionId(), ACR_PULL_ROLE_ID);
        return authorizationManager.roleAssignments()
            .listByScope(scope).stream()
            .filter(assignment -> StringUtils.equalsIgnoreCase(assignment.principalId(), identityId) &&
                StringUtils.equalsIgnoreCase(assignment.roleDefinitionId(), roleDefinitionId))
            .findFirst().orElse(null);
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    @Override
    @Nullable
    public ScaleConfig getScaleConfig() {
        return Optional.ofNullable(config).map(Config::getScaleConfig).orElse(super.getScaleConfig());
    }

    @Override
    @Nullable
    public IngressConfig getIngressConfig() {
        return Optional.ofNullable(config).map(Config::getIngressConfig).orElse(super.getIngressConfig());
    }

    @Override
    @Nullable
    public ImageConfig getImageConfig() {
        return Optional.ofNullable(config).map(Config::getImageConfig).orElse(super.getImageConfig());
    }

    @Override
    @Nullable
    public RevisionMode getRevisionMode() {
        return Optional.ofNullable(config).map(Config::getRevisionMode).orElse(super.getRevisionMode());
    }

    @Nullable
    @Override
    public ContainerAppsEnvironment getManagedEnvironment() {
        return Optional.ofNullable(config).map(Config::getEnvironment).orElseGet(super::getManagedEnvironment);
    }

    @Nullable
    @Override
    public String getManagedEnvironmentId() {
        return Optional.ofNullable(config).map(Config::getEnvironment).map(ContainerAppsEnvironment::getId).orElseGet(super::getManagedEnvironmentId);
    }

    @Nullable
    @Override
    public Region getRegion() {
        return Optional.ofNullable(config).map(Config::getEnvironment).map(ContainerAppsEnvironment::getRegion).orElseGet(super::getRegion);
    }

    @Override
    public boolean isIngressEnabled() {
        return Optional.ofNullable(config).map(Config::getIngressConfig).map(IngressConfig::isEnableIngress).orElseGet(super::isIngressEnabled);
    }

    public ResourceConfiguration getResourceConfiguration() {
        return Optional.ofNullable(config).map(Config::getResourceConfiguration).orElseGet(super::getResourceConfiguration);
    }

    @Override
    public boolean isModified() {
        return this.config == null || Objects.equals(this.config, new Config());
    }

    @Data
    public static class Config {
        private String name;
        private Subscription subscription;
        private ResourceGroup resourceGroup;
        @Nullable
        private ContainerAppsEnvironment environment;
        private RevisionMode revisionMode = RevisionMode.SINGLE;
        @Nullable
        private ImageConfig imageConfig;
        @Nullable
        private IngressConfig ingressConfig;
        @Nullable
        private ScaleConfig scaleConfig;
        @Nullable
        private ResourceConfiguration resourceConfiguration;
    }

    @Setter
    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ImageConfig {
        @Nonnull
        @EqualsAndHashCode.Include
        private String fullImageName;
        @Nullable
        private ContainerRegistry containerRegistry;
        @Nonnull
        private List<EnvironmentVar> environmentVariables = new ArrayList<>();
        @Nullable
        private BuildImageConfig buildImageConfig;
        @Nullable
        private String identity;

        public ImageConfig(@Nonnull String fullImageName) {
            this.fullImageName = fullImageName;
        }

        public String getTag() {
            return Optional.of(fullImageName.substring(fullImageName.lastIndexOf(':') + 1)).filter(StringUtils::isNotBlank).orElse("latest");
        }

        public String getRegistryUrl() {
            return fullImageName.substring(0, fullImageName.indexOf('/'));
        }

        @Nullable
        public String getAcrRegistryName() {
            final String registryUrl = this.getRegistryUrl();
            if (registryUrl.endsWith(ACR_IMAGE_SUFFIX)) {
                return registryUrl.substring(0, registryUrl.length() - ACR_IMAGE_SUFFIX.length());
            }
            return null;
        }

        public String getAcrImageNameWithTag() {
            return fullImageName.substring(fullImageName.indexOf('/') + 1);
        }

        public boolean sourceHasDockerFile() {
            return Optional.ofNullable(buildImageConfig).map(BuildImageConfig::sourceHasDockerFile).orElse(false);
        }

        public static List<Container> toContainers(@Nonnull final ImageConfig config) {
            return toContainers(config, null);
        }

        public static List<Container> toContainers(@Nonnull final ImageConfig config, @Nullable ResourceConfiguration resource) {
            final String imageId = config.getFullImageName();
            final String containerName = getContainerNameForImage(imageId);
            // drop old containers because we want to replace the old image
            final Container container = new Container().withName(containerName).withImage(imageId).withEnv(config.getEnvironmentVariables());
            if (Objects.nonNull(resource)) {
                final ContainerResources containerResources = new ContainerResources();
                containerResources.withCpu(resource.getCpu());
                containerResources.withMemory(resource.getMemory());
                container.withResources(containerResources);
            }
            return Collections.singletonList(container);
        }

        private static String getContainerNameForImage(String containerImageName) {
            final String name = containerImageName.substring(containerImageName.lastIndexOf('/') + 1).replaceAll("[^0-9a-zA-Z-]", "-").toLowerCase();
            // The length of container name can not be more than 46.
            return StringUtils.substring(name, 0, 46);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class BuildImageConfig {
        @Nonnull
        private Path source;
        private Map<String, String> sourceBuildEnv;

        public boolean sourceHasDockerFile() {
            return Optional.of(source)
                .filter(Files::isDirectory)
                .map(p -> Files.isRegularFile(Paths.get(p.toString(), "Dockerfile"))).orElse(false);
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ScaleConfig {
        @EqualsAndHashCode.Include
        private Integer maxReplicas;
        @Builder.Default
        @EqualsAndHashCode.Include
        private Integer minReplicas = 1;

        public static Scale toScale(ScaleConfig config) {
            return Optional.ofNullable(config).map(s -> new Scale().withMinReplicas(s.minReplicas).withMaxReplicas(s.maxReplicas)).orElse(null);
        }
    }
}
