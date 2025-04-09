/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.management.serializer.SerializerFactory;
import com.azure.core.util.serializer.SerializerAdapter;
import com.azure.core.util.serializer.SerializerEncoding;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.fluent.models.SiteConfigInner;
import com.azure.resourcemanager.appservice.fluent.models.SiteInner;
import com.azure.resourcemanager.appservice.models.FunctionApp.DefinitionStages;
import com.azure.resourcemanager.appservice.models.FunctionApp.Update;
import com.azure.resourcemanager.appservice.models.ManagedServiceIdentity;
import com.azure.resourcemanager.appservice.models.ManagedServiceIdentityType;
import com.azure.resourcemanager.appservice.models.NameValuePair;
import com.azure.resourcemanager.appservice.models.ResourceConfig;
import com.azure.resourcemanager.appservice.models.UserAssignedIdentity;
import com.azure.resourcemanager.authorization.AuthorizationManager;
import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.azure.resourcemanager.msi.MsiManager;
import com.azure.resourcemanager.msi.models.Identity;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.model.ContainerAppFunctionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.StorageAuthenticationMethod;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
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
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.model.EnvironmentType;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.appservice.model.Runtime.FUNCTION_UPGRADE_RUNTIME_LINK;

public class FunctionAppDraft extends FunctionApp implements AzResource.Draft<FunctionApp, com.azure.resourcemanager.appservice.models.FunctionApp> {
    private static final String CREATE_NEW_FUNCTION_APP = "isCreateNewFunctionApp";
    public static final String FUNCTIONS_EXTENSION_VERSION = "FUNCTIONS_EXTENSION_VERSION";
    public static final String UNSUPPORTED_OPERATING_SYSTEM = "Unsupported operating system %s";
    public static final String CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS = "Can not update the operation system of an existing app";

    public static final String APP_SETTING_MACHINEKEY_DECRYPTION_KEY = "MACHINEKEY_DecryptionKey";
    public static final String APP_SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE = "WEBSITES_ENABLE_APP_SERVICE_STORAGE";
    public static final String APP_SETTING_DISABLE_WEBSITES_APP_SERVICE_STORAGE = "false";
    public static final String APP_SETTING_FUNCTION_APP_EDIT_MODE = "FUNCTION_APP_EDIT_MODE";
    public static final String APP_SETTING_FUNCTION_APP_EDIT_MODE_READONLY = "readOnly";
    public static final String APPLICATIONINSIGHTS_ENABLE_AGENT = "APPLICATIONINSIGHTS_ENABLE_AGENT";
    public static final String SERVICE_PLAN_MISSING_MESSAGE = "'service plan' is required to create a Function App";
    public static final String UNSUPPORTED_OPERATING_SYSTEM_FOR_CONTAINER_APP = "Unsupported operating system %s for function app on container app";
    public static final String STORAGE_BLOB_DATA_CONTRIBUTOR_ROLE_ID = "ba92f5b4-2d11-453d-a403-e96b0029c9fe";
    public static final String ROLE_NOT_AFFECT = "The role '%s' has been assigned to the managed identity '%s'. This process may take several minutes to take effect. " +
        "If you encounter any exceptions during deployment, please try again later.";

    @Getter
    @Nullable
    private final FunctionApp origin;
    @Nullable
    private Config config;

    FunctionAppDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull FunctionAppModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    FunctionAppDraft(@Nonnull FunctionApp origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/function.create_app.app", params = {"this.getName()"})
    public com.azure.resourcemanager.appservice.models.FunctionApp createResourceInAzure() {
        Runtime.tryWarningDeprecation(this);
        OperationContext.action().setTelemetryProperty(CREATE_NEW_FUNCTION_APP, String.valueOf(true));
        OperationContext.action().setTelemetryProperty("subscriptionId", getSubscriptionId());
        OperationContext.action().setTelemetryProperty("useEnvironment", String.valueOf(Objects.nonNull(getEnvironment())));
        Optional.ofNullable(getRegion()).ifPresent(region -> OperationContext.action().setTelemetryProperty("region", region.getLabel()));
        Optional.ofNullable(getRuntime()).ifPresent(runtime -> OperationContext.action().setTelemetryProperty("runtime", runtime.getDisplayName()));
        Optional.ofNullable(getRuntime()).map(Runtime::getOperatingSystem).ifPresent(os -> OperationContext.action().setTelemetryProperty("os", os.getValue()));
        Optional.ofNullable(getRuntime()).map(Runtime::getJavaVersionUserText).ifPresent(javaVersion -> OperationContext.action().setTelemetryProperty("javaVersion", javaVersion));

        final boolean isFlexConsumption = Optional.ofNullable(getAppServicePlan())
            .map(AppServicePlan::getPricingTier)
            .map(PricingTier::isFlexConsumption).orElse(false);

        final String name = getName();
        final FunctionAppRuntime newRuntime = Objects.requireNonNull(getRuntime(), "'runtime' is required to create a Function App");
        final OffsetDateTime endOfLifeDate = newRuntime.getEndOfLifeDate();
        if (BooleanUtils.isNotTrue(getSkipEndOfLifeValidation()) && Objects.nonNull(endOfLifeDate) && OffsetDateTime.now().isAfter(endOfLifeDate)) {
            final String message = String.format("The runtime '%s' has reached end of life. " +
                "Please upgrade to a newer version. Learn more: %s", newRuntime.getDisplayName(), FUNCTION_UPGRADE_RUNTIME_LINK);
            throw new AzureToolkitRuntimeException(message);
        }

        @Nullable final AppServicePlan newPlan = getAppServicePlan();
        final ContainerAppsEnvironment environment = getEnvironment();
        final Map<String, String> newAppSettings = getAppSettings();
        final DiagnosticConfig newDiagnosticConfig = getDiagnosticConfig();
        final String funcExtVersion = Optional.ofNullable(newAppSettings).map(map -> map.get(FUNCTIONS_EXTENSION_VERSION)).orElse(null);
        final StorageAccount storageAccount = getStorageAccount();

        final AppServiceManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final DefinitionStages.Blank blank = manager.functionApps().define(name);
        final DefinitionStages.WithCreate withCreate;
        if (Objects.nonNull(environment)) {
            // container app based function app
            final ContainerAppFunctionConfiguration containerConfiguration = getContainerConfiguration();
            final Region region = Objects.requireNonNull(getRegion(), "'region' is required to create a container based function app");
            final String strRegion = StringUtils.endsWith(region.getAbbreviation(), "(stage)") ? region.getAbbreviation() : region.getName();
            final DefinitionStages.WithScaleRulesOrDockerContainerImage withImage = blank
                .withRegion(strRegion)
                .withExistingResourceGroup(getResourceGroupName())
                .withManagedEnvironmentId(environment.getId());
            Optional.ofNullable(containerConfiguration).map(ContainerAppFunctionConfiguration::getMaxReplicas).ifPresent(withImage::withMaxReplicas);
            Optional.ofNullable(containerConfiguration).map(ContainerAppFunctionConfiguration::getMinReplicas).ifPresent(withImage::withMinReplicas);
            if (newRuntime.getOperatingSystem() != OperatingSystem.DOCKER && newRuntime.getOperatingSystem() != OperatingSystem.LINUX) {
                throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM_FOR_CONTAINER_APP, newRuntime.getOperatingSystem()));
            }
            withCreate = newRuntime.getOperatingSystem() == OperatingSystem.DOCKER ?
                defineDockerContainerImage(withImage) :
                withImage.withBuiltInImage(((FunctionAppLinuxRuntime) newRuntime).toFunctionRuntimeStack(funcExtVersion));
            // workload profile configurations
            if (environment.getEnvironmentType() == EnvironmentType.WorkloadProfiles) {
                final String profile = Optional.ofNullable(containerConfiguration).map(ContainerAppFunctionConfiguration::getWorkloadProfileMame).orElse(null);
                final SiteInner siteInner = ((com.azure.resourcemanager.appservice.models.FunctionApp) withCreate).innerModel();
                siteInner.withWorkloadProfileName(StringUtils.isBlank(profile) ? "Consumption" : profile); // if profile not set, use Consumption
                if (Objects.nonNull(containerConfiguration) && ObjectUtils.anyNotNull(containerConfiguration.getCpu(), containerConfiguration.getMemory())) {
                    final ResourceConfig resourceConfig = new ResourceConfig();
                    resourceConfig.withCpu(containerConfiguration.getCpu());
                    resourceConfig.withMemory(containerConfiguration.getMemory());
                    siteInner.withResourceConfig(resourceConfig);
                }
            }
        } else {
            // normal function app
            final OperatingSystem os = newRuntime.isDocker() ? OperatingSystem.LINUX : newRuntime.getOperatingSystem();
            if (os != Objects.requireNonNull(newPlan, SERVICE_PLAN_MISSING_MESSAGE).getOperatingSystem()) {
                throw new AzureToolkitRuntimeException(String.format("Could not create %s app service in %s service plan", newRuntime.getOperatingSystem(), newPlan.getOperatingSystem()));
            }
            if (newRuntime.getOperatingSystem() == OperatingSystem.LINUX) {
                withCreate = blank.withExistingLinuxAppServicePlan(newPlan.getRemote())
                    .withExistingResourceGroup(getResourceGroupName())
                    .withBuiltInImage(((FunctionAppLinuxRuntime) newRuntime).toFunctionRuntimeStack(funcExtVersion));
            } else if (newRuntime.getOperatingSystem() == OperatingSystem.WINDOWS) {
                withCreate = (DefinitionStages.WithCreate) blank
                    .withExistingAppServicePlan(newPlan.getRemote())
                    .withExistingResourceGroup(getResourceGroupName())
                    .withJavaVersion(newRuntime.getJavaVersion())
                    .withWebContainer(null);
            } else if (newRuntime.getOperatingSystem() == OperatingSystem.DOCKER) {
                if (StringUtils.equalsIgnoreCase(newPlan.getPricingTier().getTier(), "Dynamic")) {
                    throw new AzureToolkitRuntimeException("Docker function is not supported in consumption service plan");
                }
                final DefinitionStages.WithDockerContainerImage withLinuxAppFramework = blank
                    .withExistingLinuxAppServicePlan(newPlan.getRemote())
                    .withExistingResourceGroup(getResourceGroupName());
                withCreate = defineDockerContainerImage(withLinuxAppFramework);
            } else {
                throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
            }
        }
        if (MapUtils.isNotEmpty(newAppSettings)) {
            // todo: support remove app settings
            withCreate.withAppSettings(newAppSettings);
        }
        if (Objects.nonNull(storageAccount)) {
            withCreate.withExistingStorageAccount(storageAccount.getRemote());
        }
        // diagnostic config is only available for service plan function apps
        if (Objects.nonNull(newDiagnosticConfig) && Objects.isNull(environment)) {
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig);
        }
        final Boolean enableDistributedTracing = ensureConfig().getEnableDistributedTracing();
        if (Objects.nonNull(enableDistributedTracing)) {
            withCreate.withAppSetting(APPLICATIONINSIGHTS_ENABLE_AGENT, String.valueOf(enableDistributedTracing));
        } else if (shouldEnableDistributedTracing(newPlan, newAppSettings)) {
            withCreate.withAppSetting(APPLICATIONINSIGHTS_ENABLE_AGENT, "true");
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Function App({0})...", name));
        com.azure.resourcemanager.appservice.models.FunctionApp functionApp = Objects.requireNonNull(this.doModify(() -> {
            if (isFlexConsumption) {
                return createOrUpdateFlexConsumptionFunctionAppWithRawRequest((com.azure.resourcemanager.appservice.models.FunctionApp) withCreate);
                // todo: update identity configuration once service supports identity authentication,
                // return updateFlexFunctionAppIdentityConfiguration(app, Objects.requireNonNull(getFlexConsumptionConfiguration()));
            } else {
                return withCreate.create();
            }
        }, Status.CREATING));
        // deploy action did not fit docker runtime function app
        final Action<AzResource> deploy = getRuntime().isDocker() ? null : Optional.ofNullable(AzureActionManager.getInstance().getAction(AzResource.DEPLOY))
            .map(action -> action.bind(this)).orElse(null);
        messager.success(AzureString.format("Function App({0}) is successfully created", name), deploy);
        Optional.ofNullable(getEnvironment()).ifPresent(ContainerAppsEnvironment::refresh); // refresh container apps environment after create container hosted function app
        return functionApp;
    }

    // todo: remove outdated codes after stable sdk which support flex consumption released
    private void updateFlexFunctionAppIdentityConfiguration(final com.azure.resourcemanager.appservice.models.FunctionApp app, final FunctionAppConfig.Storage.Authentication configuration) throws IOException {
        final AppServiceManager manager = app.manager();
        // as we can't call sdk create/update method to create identity, we need to set the inner model manually and grant permissions manually later
        // Refers WebAppMsiHandler, WebAppBaseImpl
        if (configuration.getType() == StorageAuthenticationMethod.SystemAssignedIdentity) {
            app.innerModel().withIdentity(new ManagedServiceIdentity().withType(ManagedServiceIdentityType.SYSTEM_ASSIGNED));
            // authorizationManager.roleAssignments(storageAccount.getId(), BuiltInRole.STORAGE_BLOB_DATA_CONTRIBUTOR)
        } else if (configuration.getType() == StorageAuthenticationMethod.UserAssignedIdentity) {
            final Subscription subscription = Azure.az(AzureAccount.class).account().getSubscription(getSubscriptionId());
            final MsiManager msiManager = MsiManager.authenticate(manager.httpPipeline(), new AzureProfile(subscription.getTenantId(), subscription.getId(), manager.environment()));
            final Identity identity = msiManager.identities().getById(configuration.getUserAssignedIdentityResourceId());
            final String identityJson = String.format("{\"principalId\" : \"%s\", \"clientId\" : \"%s\"}", identity.principalId(), identity.clientId());
            final SerializerAdapter adapter = SerializerFactory.createDefaultManagementSerializerAdapter();
            // todo: sync with sdk team to find a better way to create user identity object
            final UserAssignedIdentity userAssignedIdentity = adapter.deserialize(identityJson, UserAssignedIdentity.class, SerializerEncoding.JSON);
            app.innerModel().withIdentity(new ManagedServiceIdentity()
                .withType(ManagedServiceIdentityType.USER_ASSIGNED)
                .withUserAssignedIdentities(Collections.singletonMap(identity.id(), userAssignedIdentity)));
        }
    }

    private void grantPermissionToIdentity(final com.azure.resourcemanager.appservice.models.FunctionApp result) {
        final FunctionAppConfig config = getFlexConfigFromRemote(result);
        final FunctionAppConfig.Storage storage = config.getDeployment().getStorage();
        final FunctionAppConfig.Storage.Authentication authConfiguration = Objects.requireNonNull(storage.getAuthentication());
        final StorageAccount storageAccount = Optional.ofNullable(ensureConfig().getDeploymentAccount())
            .orElseGet(() -> ensureConfig().getStorageAccount());
        if (Objects.isNull(storageAccount)) {
            return;
        }
        final String identityId = authConfiguration.getType() == StorageAuthenticationMethod.SystemAssignedIdentity ?
            result.identity().principalId() :
            result.identity().userAssignedIdentities().entrySet().stream()
                .filter(entry -> StringUtils.equalsIgnoreCase(entry.getKey(), authConfiguration.getUserAssignedIdentityResourceId()))
                .findFirst()
                .map(Map.Entry::getValue)
                .map(UserAssignedIdentity::principalId).orElseThrow(()-> new RuntimeException("User assigned identity not found"));
        final String roleAssignmentName = UUID.randomUUID().toString();
        final String scope = storageAccount.getId();
        final RoleAssignment existingAssignment = getExistingRoleAssignment(identityId, scope);
        if (Objects.isNull(existingAssignment)) {
            try {
                final AuthorizationManager authorizationManager = Objects.requireNonNull(this.getParent().getRemote()).authorizationManager();
                authorizationManager.roleAssignments().define(roleAssignmentName)
                    .forObjectId(identityId)
                    .withBuiltInRole(BuiltInRole.STORAGE_BLOB_DATA_CONTRIBUTOR)
                    .withScope(scope).create();
                final RoleAssignment existingRoleAssignment = getExistingRoleAssignment(identityId, scope);
                if (Objects.isNull(existingRoleAssignment)) {
                    AzureMessager.getMessager().error(String.format(ROLE_NOT_AFFECT, BuiltInRole.STORAGE_BLOB_DATA_CONTRIBUTOR, identityId));
                }
            } catch (final Throwable t) {
                final String message = String.format("Failed to assign role '%s' to managed identity '%s', please assign the role manually or your app may not able to work : %s",
                    BuiltInRole.STORAGE_BLOB_DATA_CONTRIBUTOR, identityId, ExceptionUtils.getMessage(t));
                AzureMessager.getMessager().error(message, t);
            }
        } // ba92f5b4-2d11-453d-a403-e96b0029c9fe
    }

    private RoleAssignment getExistingRoleAssignment(final String identityId, final String scope) {
        final AuthorizationManager authorizationManager = Objects.requireNonNull(this.getParent().getRemote()).authorizationManager();
        final String roleDefinitionId = String.format("/subscriptions/%s/providers/Microsoft.Authorization/roleDefinitions/%s", getSubscriptionId(), STORAGE_BLOB_DATA_CONTRIBUTOR_ROLE_ID);
        return authorizationManager.roleAssignments()
            .listByScope(scope).stream()
            .filter(assignment -> StringUtils.equalsIgnoreCase(assignment.principalId(), identityId) &&
                StringUtils.equalsIgnoreCase(assignment.roleDefinitionId(), roleDefinitionId))
            .findFirst().orElse(null);
    }

    private boolean shouldEnableDistributedTracing(@Nullable final AppServicePlan servicePlan, @Nullable final Map<String, String> appSettings) {
        final boolean isConsumptionPlan = !Objects.isNull(servicePlan) && servicePlan.getPricingTier().isConsumption();
        final boolean isSetInAppSettings = MapUtils.isNotEmpty(appSettings) && appSettings.containsKey(APPLICATIONINSIGHTS_ENABLE_AGENT);
        return !(isConsumptionPlan || isSetInAppSettings);
    }

    DefinitionStages.WithCreate defineDockerContainerImage(@Nonnull DefinitionStages.WithDockerContainerImage withLinuxAppFramework) {
        // check service plan, consumption is not supported
        final String message = "Docker configuration is required to create a docker based Function app";
        final DockerConfiguration config = Objects.requireNonNull(this.getDockerConfiguration(), message);
        final DefinitionStages.WithCreate draft;
        if (StringUtils.isAllEmpty(config.getUserName(), config.getPassword())) {
            draft = withLinuxAppFramework
                .withPublicDockerHubImage(config.getImage());
        } else if (StringUtils.isEmpty(config.getRegistryUrl())) {
            draft = withLinuxAppFramework
                .withPrivateDockerHubImage(config.getImage())
                .withCredentials(config.getUserName(), config.getPassword());
        } else {
            draft = withLinuxAppFramework
                .withPrivateRegistryImage(config.getImage(), config.getRegistryUrl())
                .withCredentials(config.getUserName(), config.getPassword());
        }
        final String decryptionKey = generateDecryptionKey();
        return (DefinitionStages.WithCreate) draft.withAppSetting(APP_SETTING_MACHINEKEY_DECRYPTION_KEY, decryptionKey)
            .withAppSetting(APP_SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, APP_SETTING_DISABLE_WEBSITES_APP_SERVICE_STORAGE)
            .withAppSetting(APP_SETTING_FUNCTION_APP_EDIT_MODE, APP_SETTING_FUNCTION_APP_EDIT_MODE_READONLY);
    }

    protected String generateDecryptionKey() {
        // Refers https://github.com/Azure/azure-cli/blob/dev/src/azure-cli/azure/cli/command_modules/appservice/custom.py#L2300
        return Hex.encodeHexString(RandomUtils.nextBytes(32)).toUpperCase();
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/function.update_app.app", params = {"this.getName()"})
    public com.azure.resourcemanager.appservice.models.FunctionApp updateResourceInAzure(@Nonnull com.azure.resourcemanager.appservice.models.FunctionApp remote) {
        assert origin != null : "updating target is not specified.";
        Runtime.tryWarningDeprecation(this);
        final Map<String, String> oldAppSettings = Objects.requireNonNull(origin.getAppSettings());
        // remove app settings which has already existed
        final Map<String, String> settingsToAdd = Optional.ofNullable(this.ensureConfig().getAppSettings())
            .map(map -> map.entrySet().stream().filter(entry -> !StringUtils.equals(oldAppSettings.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElseGet(HashMap::new);
        final Boolean enableDistributedTracing = ensureConfig().getEnableDistributedTracing();
        if (Objects.nonNull(enableDistributedTracing)) {
            settingsToAdd.put(APPLICATIONINSIGHTS_ENABLE_AGENT, String.valueOf(enableDistributedTracing));
        }
        final Set<String> settingsToRemove = Optional.ofNullable(this.ensureConfig().getAppSettingsToRemove())
            .map(set -> set.stream().filter(oldAppSettings::containsKey).collect(Collectors.toSet()))
            .orElse(Collections.emptySet());
        final DiagnosticConfig oldDiagnosticConfig = super.getDiagnosticConfig();
        final DiagnosticConfig newDiagnosticConfig = this.ensureConfig().getDiagnosticConfig();
        final Runtime newRuntime = this.ensureConfig().getRuntime();
        @Nullable final AppServicePlan newPlan = this.ensureConfig().getPlan();
        final DockerConfiguration newDockerConfig = this.ensureConfig().getDockerConfiguration();
        final FlexConsumptionConfiguration newFlexConsumptionConfiguration = this.ensureConfig().getFlexConsumptionConfiguration();
        final StorageAccount storageAccount = getStorageAccount();
        final Runtime oldRuntime = origin.getRuntime();
        final AppServicePlan oldPlan = origin.getAppServicePlan();
        final FlexConsumptionConfiguration oldFlexConsumptionConfiguration = origin.getFlexConsumptionConfiguration();
        final ContainerAppFunctionConfiguration oldContainerConfiguration = origin.getContainerConfiguration();
        final ContainerAppFunctionConfiguration newContainerConfiguration = this.ensureConfig().getContainerConfiguration();

        final boolean planModified = Objects.nonNull(newPlan) && !Objects.equals(newPlan, oldPlan);
        final boolean dockerModified = Objects.nonNull(oldRuntime) && oldRuntime.isDocker() &&
            Objects.nonNull(newDockerConfig);
        final boolean isFlexConsumption = Optional.ofNullable(getAppServicePlan())
            .map(AppServicePlan::getPricingTier).map(PricingTier::isFlexConsumption).orElse(false);
        final boolean flexConsumptionModified = isFlexConsumption &&
            Objects.nonNull(newFlexConsumptionConfiguration) && isFlexConsumptionModified(oldFlexConsumptionConfiguration, newFlexConsumptionConfiguration);
        final boolean isAppSettingsModified = MapUtils.isNotEmpty(settingsToAdd) || CollectionUtils.isNotEmpty(settingsToRemove);
        final boolean isDiagnosticConfigModified = Objects.nonNull(newDiagnosticConfig) && !Objects.equals(newDiagnosticConfig, oldDiagnosticConfig);
        final boolean runtimeModified = !isFlexConsumption && (Objects.isNull(oldRuntime) || !oldRuntime.isDocker()) &&
            Objects.nonNull(newRuntime) && !Objects.equals(newRuntime, oldRuntime);
        final boolean envConfigurationModified = isContainerHostingFunctionApp() && Objects.nonNull(newContainerConfiguration) &&
            !Objects.equals(oldContainerConfiguration, newContainerConfiguration);
        final boolean modified = planModified || runtimeModified || dockerModified || flexConsumptionModified ||
            isAppSettingsModified || Objects.nonNull(newDiagnosticConfig) || Objects.nonNull(storageAccount) || envConfigurationModified;
        final String funcExtVersion = Optional.of(settingsToAdd).map(map -> map.get(FUNCTIONS_EXTENSION_VERSION))
            .orElseGet(() -> oldAppSettings.get(FUNCTIONS_EXTENSION_VERSION));
        if (modified) {
            final Update update = remote.update();
            Optional.ofNullable(newPlan).filter(ignore -> planModified).ifPresent(p -> updateAppServicePlan(update, p));
            Optional.ofNullable(newRuntime).filter(ignore -> runtimeModified).ifPresent(p -> updateRuntime(update, p, funcExtVersion));
            Optional.of(settingsToAdd).filter(ignore -> isAppSettingsModified).ifPresent(update::withAppSettings);
            Optional.of(settingsToRemove).filter(CollectionUtils::isNotEmpty).filter(ignore -> isAppSettingsModified).ifPresent(s -> s.forEach(update::withoutAppSetting));
            Optional.ofNullable(newDockerConfig).filter(ignore -> dockerModified).ifPresent(p -> updateDockerConfiguration(update, p));
            Optional.ofNullable(newDiagnosticConfig).filter(ignore -> isDiagnosticConfigModified).filter(ignore -> StringUtils.isBlank(getEnvironmentId())).ifPresent(c -> AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, c));
            Optional.ofNullable(newFlexConsumptionConfiguration).filter(ignore -> flexConsumptionModified).ifPresent(c -> update.withContainerSize(c.getInstanceSize()));
            Optional.ofNullable(storageAccount).ifPresent(s -> update.withExistingStorageAccount(s.getRemote()));
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating Function App({0})...", remote.name()));
            remote = Objects.requireNonNull(this.doModify(() -> {
                if (isFlexConsumption) {
                    return createOrUpdateFlexConsumptionFunctionAppWithRawRequest((com.azure.resourcemanager.appservice.models.FunctionApp) update);
                    // todo: update identity configuration once service supports identity authentication,
                    // return updateFlexFunctionAppIdentityConfiguration(app, Objects.requireNonNull(newFlexConsumptionConfiguration));
                } else {
                    final com.azure.resourcemanager.appservice.models.FunctionApp result = update.apply();
                    if (envConfigurationModified) {
                        final Update containerUpdate = result.update();
                        Optional.ofNullable(newContainerConfiguration).ifPresent(c -> updateContainerFunctionConfiguration(containerUpdate, c));
                        return ((DefinitionStages.WithCreate) containerUpdate).create();
                    }
                    return result;
                }
            }, Status.CREATING));
            messager.success(AzureString.format("Function App({0}) is successfully updated", remote.name()));
        }
        return remote;
    }

    private void updateContainerFunctionConfiguration(final Update update, final ContainerAppFunctionConfiguration c) {
        Optional.ofNullable(c.getMaxReplicas()).ifPresent(update::withMaxReplicas);
        Optional.ofNullable(c.getMinReplicas()).ifPresent(update::withMinReplicas);
        final SiteInner siteInner = ((com.azure.resourcemanager.appservice.models.FunctionApp) update).innerModel();
        Optional.ofNullable(c.getWorkloadProfileMame()).filter(StringUtils::isNotBlank).ifPresent(siteInner::withWorkloadProfileName);
        final ResourceConfig resourceConfig = Optional.ofNullable(siteInner.resourceConfig()).orElseGet(ResourceConfig::new);
        Optional.ofNullable(c.getCpu()).ifPresent(resourceConfig::withCpu);
        Optional.ofNullable(c.getMemory()).ifPresent(resourceConfig::withMemory);
    }

    private boolean isFlexConsumptionModified(final FlexConsumptionConfiguration oldConfiguration, final FlexConsumptionConfiguration newConfiguration) {
        if (Objects.isNull(newConfiguration)) {
            return false;
        }
        if (Objects.isNull(oldConfiguration)) {
            return true;
        }
        return (Objects.nonNull(newConfiguration.getInstanceSize()) && !Objects.equals(oldConfiguration.getInstanceSize(), newConfiguration.getInstanceSize())) ||
            (Objects.nonNull(newConfiguration.getMaximumInstances()) && !Objects.equals(oldConfiguration.getMaximumInstances(), newConfiguration.getMaximumInstances())) ||
            (Objects.nonNull(newConfiguration.getAlwaysReadyInstances()) && !Arrays.equals(oldConfiguration.getAlwaysReadyInstances(), newConfiguration.getAlwaysReadyInstances())) ||
            (Objects.nonNull(newConfiguration.getDeploymentContainer()) && !Objects.equals(oldConfiguration.getDeploymentContainer(), newConfiguration.getDeploymentContainer())) ||
            (Objects.nonNull(newConfiguration.getDeploymentResourceGroup()) && !Objects.equals(oldConfiguration.getDeploymentResourceGroup(), newConfiguration.getDeploymentResourceGroup())) ||
            (Objects.nonNull(newConfiguration.getDeploymentAccount()) && !Objects.equals(oldConfiguration.getDeploymentAccount(), newConfiguration.getDeploymentAccount())) ||
            (Objects.nonNull(newConfiguration.getAuthenticationMethod()) && !Objects.equals(oldConfiguration.getAuthenticationMethod(), newConfiguration.getAuthenticationMethod())) ||
            (Objects.nonNull(newConfiguration.getUserAssignedIdentityResourceId()) && !Objects.equals(oldConfiguration.getUserAssignedIdentityResourceId(), newConfiguration.getUserAssignedIdentityResourceId())) ||
            (Objects.nonNull(newConfiguration.getStorageAccountConnectionString()) && !Objects.equals(oldConfiguration.getStorageAccountConnectionString(), newConfiguration.getStorageAccountConnectionString()));
    }

    private com.azure.resourcemanager.appservice.models.FunctionApp createOrUpdateFlexConsumptionFunctionAppWithRawRequest(com.azure.resourcemanager.appservice.models.FunctionApp functionApp) throws IOException {
        // serialize inner object into json
        final FunctionAppConfig flexConfig = getFlexConsumptionAppConfig();
        final SiteInner siteInner = functionApp.innerModel();
        // clean up deprecated properties
        updateSiteConfigurations(functionApp, flexConfig);

        final FunctionAppConfig.Storage.Authentication authentication = Optional.of(flexConfig).map(FunctionAppConfig::getDeployment).map(FunctionAppConfig.FunctionsDeployment::getStorage)
            .map(FunctionAppConfig.Storage::getAuthentication).orElse(null);
        final boolean isManageIdentityAuthentication = Objects.nonNull(authentication) && authentication.getType() != StorageAuthenticationMethod.StorageAccountConnectionString;
        if (isManageIdentityAuthentication) {
            updateFlexFunctionAppIdentityConfiguration(functionApp, authentication);
        }

        final SerializerAdapter adapter = SerializerFactory.createDefaultManagementSerializerAdapter();
        final String originContent = adapter.serializeRaw(siteInner);
        final ObjectNode jsonNode = adapter.deserialize(originContent, ObjectNode.class, SerializerEncoding.JSON);
        // update json object
        final ObjectNode configNode = adapter.deserialize(adapter.serializeRaw(flexConfig), ObjectNode.class, SerializerEncoding.JSON);
        final ObjectNode properties = ((ObjectNode) jsonNode.get("properties"));
        properties.set("functionAppConfig", configNode);
        Optional.ofNullable(getAppServicePlan()).map(AppServicePlan::getPricingTier)
            .ifPresent(tier -> properties.put("sku", tier.getTier()));
        // add flex consumption configuration
        final String newContent = adapter.serializeRaw(jsonNode);
        // submit raw http request
        final HttpPipeline httpPipeline = functionApp.manager().httpPipeline();
        final String targetUrl = getRawRequestEndpoint(functionApp);
        final HttpMethod method = this.exists() ? HttpMethod.PATCH : HttpMethod.PUT;
        final HttpRequest request = new HttpRequest(method, targetUrl)
            .setHeader(HttpHeaderName.CONTENT_TYPE, "application/json")
            .setBody(newContent);
        try (final HttpResponse response = httpPipeline.send(request).block()) {
            if (Objects.isNull(response) || response.getStatusCode() >= 300 || response.getStatusCode() < 200) {
                final String content = Objects.isNull(response) ? StringUtils.EMPTY : response.getBodyAsString().block();
                throw new AzureToolkitRuntimeException(String.format("Failed to create or update function app : %s", content));
            }
            // get config from azure, so that we could get the new created managed identities info
            final com.azure.resourcemanager.appservice.models.FunctionApp result =
                functionApp.manager().functionApps().getByResourceGroup(functionApp.resourceGroupName(), functionApp.name());
            // workaround to avoid identity properties is not updated
            result.refresh();
            if (isManageIdentityAuthentication) {
                grantPermissionToIdentity(result);
            }
            return result;
        } catch (Throwable t) {
            throw new AzureToolkitRuntimeException(t);
        }
    }

    private void updateSiteConfigurations(@Nonnull final com.azure.resourcemanager.appservice.models.FunctionApp app, final FunctionAppConfig flexConfig) {
        // add missing properties which was handled by sdk commit before
        final SiteInner siteInner = app.innerModel();
        final SiteConfigInner siteConfigInner = Optional.ofNullable(siteInner.siteConfig()).orElseGet(SiteConfigInner::new);
        siteInner.withSiteConfig(siteConfigInner);
        // app settings
        final Map<String, String> appSettings = this.exists() ? Utils.normalizeAppSettings(app.getAppSettings()) : new HashMap<>();
        Optional.ofNullable(ensureConfig().getAppSettings()).ifPresent(appSettings::putAll);
        Optional.ofNullable(ensureConfig().getAppSettingsToRemove()).ifPresent(values -> values.forEach(appSettings::remove));
        // AzureWebJobsStorage
        final StorageAccount storageAccount = getStorageAccount();
        Optional.ofNullable(storageAccount).ifPresent(account -> appSettings.put("AzureWebJobsStorage", account.getConnectionString()));
        // DEPLOYMENT_STORAGE_CONNECTION_STRING
        final FunctionAppConfig.Storage.Authentication authentication = flexConfig.getDeployment().getStorage().getAuthentication();
        if (authentication.getType() == StorageAuthenticationMethod.StorageAccountConnectionString) {
            final StorageAccount deploymentAccount = ensureConfig().getDeploymentAccount();
            Optional.ofNullable(deploymentAccount).ifPresent(account ->
                appSettings.put(authentication.getStorageAccountConnectionStringName(), account.getConnectionString()));
        }
        // Remove deprecated app settings
        // Refers Flex Consumption Portal and Dev Tooling Public Preview Spec
        appSettings.remove("FUNCTIONS_EXTENSION_VERSION");
        appSettings.remove("FUNCTIONS_WORKER_RUNTIME");
        appSettings.remove("FUNCTIONS_WORKER_RUNTIME_VERSION");
        appSettings.remove("FUNCTIONS_MAX_HTTP_CONCURRENCY");
        appSettings.remove("FUNCTIONS_WORKER_PROCESS_COUNT");
        appSettings.remove("FUNCTIONS_WORKER_DYNAMIC_CONCURRENCY_ENABLED");
        appSettings.remove("WEBSITE_CONTENTAZUREFILECONNECTIONSTRING");
        appSettings.remove("WEBSITE_CONTENTSHARE");
        final List<NameValuePair> settings = appSettings.entrySet().stream()
            .map(entry -> new NameValuePair().withName(entry.getKey()).withValue(entry.getValue()))
            .collect(Collectors.toList());
        siteConfigInner.withAppSettings(settings);
        siteConfigInner.withHttp20Enabled(true);
        // clean up deprecated properties
        siteInner.withHttpsOnly(false);
        siteInner.withIsXenon(null);
        siteInner.withContainerSize(null);
        siteInner.withReserved(null);
        Optional.ofNullable(siteInner.siteConfig()).ifPresent(config -> {
            config.withFtpsState(null);
            config.withUse32BitWorkerProcess(null);
            config.withWindowsFxVersion(null);
            config.withLinuxFxVersion(null);
            config.withAlwaysOn(null);
            config.withPreWarmedInstanceCount(null);
            config.withFunctionAppScaleLimit(null);
            config.withJavaVersion(null);
        });
    }

    @Override
    public FunctionAppConfig getFlexConsumptionAppConfig() {
        final FlexConsumptionConfiguration configuration = ensureConfig().getFlexConsumptionConfiguration();
        final FunctionAppConfig original = super.getFlexConsumptionAppConfig();
        if (Objects.isNull(configuration)) {
            return original;
        }
        final FunctionAppConfig result = this.isDraftForCreating() ? new FunctionAppConfig() : original;
        final FunctionAppConfig.FunctionsDeployment deployment = Optional.ofNullable(result.getDeployment()).orElseGet(FunctionAppConfig.FunctionsDeployment::new);
        final FunctionAppConfig.Storage storage = Optional.ofNullable(deployment.getStorage()).orElseGet(FunctionAppConfig.Storage::new);
        // Create FunctionsDeployment.Storage.Authentication
        Optional.ofNullable(FunctionAppConfig.Storage.Authentication.fromConfiguration(configuration)).ifPresent(storage::setAuthentication);
        Optional.ofNullable(ensureConfig().getDeploymentContainerUrl()).ifPresent(storage::setValue);
        deployment.setStorage(storage);
        result.setDeployment(deployment);
        // Create FunctionsRuntime
        final FunctionAppConfig.FunctionsRuntime functionsRuntime = Optional.ofNullable(result.getRuntime()).orElseGet(FunctionAppConfig.FunctionsRuntime::new);
        Optional.ofNullable(ensureConfig().getRuntime()).map(FunctionAppRuntime::getJavaVersionNumber).ifPresent(functionsRuntime::setVersion);
        result.setRuntime(functionsRuntime);
        // Create FunctionScaleAndConcurrency
        // todo: support other trigger concurrency settings
        final FunctionAppConfig.FunctionScaleAndConcurrency concurrency = Optional.ofNullable(result.getScaleAndConcurrency())
            .orElseGet(FunctionAppConfig.FunctionScaleAndConcurrency::new);
        Optional.ofNullable(configuration.getHttpInstanceConcurrency()).map(FunctionAppConfig.FunctionTriggers::new).ifPresent(concurrency::setTriggers);
        Optional.ofNullable(configuration.getInstanceSize()).ifPresent(concurrency::setInstanceMemoryMB);
        Optional.ofNullable(configuration.getMaximumInstances()).ifPresent(concurrency::setMaximumInstanceCount);
        Optional.ofNullable(configuration.getAlwaysReadyInstances()).ifPresent(concurrency::setAlwaysReady);
        result.setScaleAndConcurrency(concurrency);
        return result;
    }

    private void updateAppServicePlan(@Nonnull Update update, @Nonnull AppServicePlan newPlan) {
        Objects.requireNonNull(newPlan.getRemote(), "Target app service plan doesn't exist");
        final OperatingSystem os = Objects.requireNonNull(getRuntime()).isDocker() ? OperatingSystem.LINUX : getRuntime().getOperatingSystem();
        if (os != newPlan.getOperatingSystem()) {
            throw new AzureToolkitRuntimeException(String.format("Could not migrate %s app service to %s service plan", getRuntime().getOperatingSystem(), newPlan.getOperatingSystem()));
        }
        update.withExistingAppServicePlan(newPlan.getRemote());
    }

    private void updateRuntime(@Nonnull Update update, @Nonnull Runtime newRuntime, String funcExtVersion) {
        final Runtime oldRuntime = Objects.requireNonNull(Objects.requireNonNull(origin).getRuntime());
        if (newRuntime.getOperatingSystem() != null && oldRuntime.getOperatingSystem() != newRuntime.getOperatingSystem()) {
            throw new AzureToolkitRuntimeException(CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS);
        }
        final OperatingSystem operatingSystem = ObjectUtils.firstNonNull(newRuntime.getOperatingSystem(), oldRuntime.getOperatingSystem());
        if (operatingSystem == OperatingSystem.LINUX) {
            update.withBuiltInImage(((FunctionAppLinuxRuntime) newRuntime).toFunctionRuntimeStack(funcExtVersion));
        } else if (operatingSystem == OperatingSystem.WINDOWS) {
            update.withJavaVersion(newRuntime.getJavaVersion()).withWebContainer(null);
        } else if (newRuntime.getOperatingSystem() == OperatingSystem.DOCKER) {
            return; // skip for docker, as docker configuration will be handled in `updateDockerConfiguration`
        } else {
            throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
        }
    }

    private void updateDockerConfiguration(@Nonnull Update update, @Nonnull DockerConfiguration newConfig) {
        if (StringUtils.isAllEmpty(newConfig.getUserName(), newConfig.getPassword())) {
            update.withPublicDockerHubImage(newConfig.getImage());
        } else if (StringUtils.isEmpty(newConfig.getRegistryUrl())) {
            update.withPrivateDockerHubImage(newConfig.getImage())
                .withCredentials(newConfig.getUserName(), newConfig.getPassword());
        } else {
            update.withPrivateRegistryImage(newConfig.getImage(), newConfig.getRegistryUrl())
                .withCredentials(newConfig.getUserName(), newConfig.getPassword());
        }
    }

    public void setRuntime(FunctionAppRuntime runtime) {
        this.ensureConfig().setRuntime(runtime);
    }

    @Nullable
    @Override
    public FunctionAppRuntime getRuntime() {
        return Optional.ofNullable(config).map(Config::getRuntime).orElseGet(super::getRuntime);
    }

    public void setStorageAccount(StorageAccount account) {
        this.ensureConfig().setStorageAccount(account);
    }

    public StorageAccount getStorageAccount() {
        return Optional.ofNullable(config).map(Config::getStorageAccount).orElse(null);
    }

    public void setAppServicePlan(AppServicePlan plan) {
        this.ensureConfig().setPlan(plan);
    }

    @Nullable
    @Override
    public AppServicePlan getAppServicePlan() {
        return Optional.ofNullable(config).map(Config::getPlan).orElseGet(super::getAppServicePlan);
    }

    public void setDiagnosticConfig(DiagnosticConfig config) {
        this.ensureConfig().setDiagnosticConfig(config);
    }

    @Nullable
    public DiagnosticConfig getDiagnosticConfig() {
        return Optional.ofNullable(config).map(Config::getDiagnosticConfig).orElseGet(super::getDiagnosticConfig);
    }

    public void setAppSettings(Map<String, String> appSettings) {
        this.ensureConfig().setAppSettings(appSettings);
    }

    public void setAppSetting(String key, String value) {
        this.ensureConfig().getAppSettings().put(key, value);
    }

    @Nullable
    @Override
    public Map<String, String> getAppSettings() {
        return Optional.ofNullable(config).map(Config::getAppSettings).orElseGet(super::getAppSettings);
    }

    public void removeAppSetting(String key) {
        this.ensureConfig().getAppSettingsToRemove().add(key);
    }

    public void removeAppSettings(Set<String> keys) {
        this.ensureConfig().getAppSettingsToRemove().addAll(ObjectUtils.firstNonNull(keys, Collections.emptySet()));
    }

    @Nullable
    public Set<String> getAppSettingsToRemove() {
        return Optional.ofNullable(config).map(Config::getAppSettingsToRemove).orElse(new HashSet<>());
    }

    public void setDockerConfiguration(DockerConfiguration dockerConfiguration) {
        this.ensureConfig().setDockerConfiguration(dockerConfiguration);
    }

    @Nullable
    public DockerConfiguration getDockerConfiguration() {
        return Optional.ofNullable(config).map(Config::getDockerConfiguration).orElse(null);
    }

    public void setFlexConsumptionConfiguration(FlexConsumptionConfiguration flexConsumptionConfiguration) {
        this.ensureConfig().setFlexConsumptionConfiguration(flexConsumptionConfiguration);
    }

    public void setEnableDistributedTracing(@Nullable final Boolean enableDistributedTracing) {
        this.ensureConfig().setEnableDistributedTracing(enableDistributedTracing);
    }

    @Nullable
    public FlexConsumptionConfiguration getFlexConsumptionConfiguration() {
        return Optional.ofNullable(config).map(Config::getFlexConsumptionConfiguration).orElseGet(super::getFlexConsumptionConfiguration);
    }

    public void setEnvironment(ContainerAppsEnvironment environment) {
        this.ensureConfig().setEnvironment(environment);
    }

    public void setContainerConfiguration(ContainerAppFunctionConfiguration containerConfiguration) {
        this.ensureConfig().setContainerConfiguration(containerConfiguration);
    }

    public ContainerAppsEnvironment getEnvironment() {
        return Optional.ofNullable(config).map(Config::getEnvironment).orElseGet(super::getEnvironment);
    }

    public String getEnvironmentId() {
        return Optional.ofNullable(config).map(Config::getEnvironment).map(ContainerAppsEnvironment::getId).orElseGet(super::getEnvironmentId);
    }

    public ContainerAppFunctionConfiguration getContainerConfiguration() {
        return Optional.ofNullable(config).map(Config::getContainerConfiguration).orElseGet(super::getContainerConfiguration);
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) ||
            Objects.isNull(this.config.getRuntime()) || Objects.equals(this.config.getRuntime(), super.getRuntime()) ||
            Objects.isNull(this.config.getPlan()) || Objects.equals(this.config.getPlan(), super.getAppServicePlan()) ||
            Objects.isNull(this.config.getDiagnosticConfig()) ||
            CollectionUtils.isEmpty(this.config.getAppSettingsToRemove()) ||
            Objects.isNull(this.config.getAppSettings()) || Objects.equals(this.config.getAppSettings(), super.getAppSettings()) ||
            Objects.isNull(this.config.getDockerConfiguration());
        return !notModified;
    }

    public void setRegion(Region region) {
        this.ensureConfig().setRegion(region);
    }

    public Region getRegion() {
        return Optional.ofNullable(config).map(Config::getRegion).orElseGet(super::getRegion);
    }

    public void setDeploymentAccount(final StorageAccount deploymentStorageAccount) {
        ensureConfig().setDeploymentAccount(deploymentStorageAccount);
    }

    public void setDeploymentContainerUrl(final String containerName) {
        ensureConfig().setDeploymentContainerUrl(containerName);
    }

    public void setSkipEndOfLifeValidation(Boolean enableEOFChecking) {
        ensureConfig().setSkipEndOfLifeValidation(enableEOFChecking);
    }

    public Boolean getSkipEndOfLifeValidation() {
        return Optional.ofNullable(config).map(Config::getSkipEndOfLifeValidation).orElse(false);
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    @Nullable
    private static class Config {
        private FunctionAppRuntime runtime;
        private Region region;
        private AppServicePlan plan = null;
        private StorageAccount storageAccount = null;
        private ContainerAppsEnvironment environment = null;
        private ContainerAppFunctionConfiguration containerConfiguration = null;
        private Boolean enableDistributedTracing = null;
        private DiagnosticConfig diagnosticConfig = null;
        private Set<String> appSettingsToRemove = new HashSet<>();
        private Map<String, String> appSettings = new HashMap<>();
        private DockerConfiguration dockerConfiguration = null;
        private FlexConsumptionConfiguration flexConsumptionConfiguration;
        private StorageAccount deploymentAccount = null;
        private String deploymentContainerUrl = null;
        // validate eof of function
        private Boolean skipEndOfLifeValidation = Boolean.TRUE;
    }
}