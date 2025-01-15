package com.microsoft.azure.maven.containerapps;

import com.microsoft.azure.maven.containerapps.config.AppContainerMavenConfig;
import com.microsoft.azure.maven.containerapps.config.DeploymentType;
import com.microsoft.azure.maven.containerapps.config.IngressMavenConfig;
import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.prompt.ConfigurationPrompter;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.maven.utils.PomUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentModule;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistryModule;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.config.ContainerRegistryConfig;
import com.microsoft.azure.toolkit.lib.identities.AzureManagedIdentity;
import com.microsoft.azure.toolkit.lib.identities.Identity;
import lombok.Lombok;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Mojo(name = "config")
public class ConfigMojo extends AbstractMojoBase {
    private static final List<String> REQUIRED_PROPERTIES = Arrays.asList("subscriptionId", "resourceGroup", "appEnvironmentName", "appName");

    /**
     * The prompt wrapper to get user input for each property.
     */
    private ConfigurationPrompter wrapper;

    /**
     * The maven variable used to evaluate maven expression.
     */
    @Parameter(defaultValue = "${mojoExecution}")
    protected MojoExecution mojoExecution;

    private boolean useExistingAppEnv = false;

    @Override
    @AzureOperation("user/containerapps.config_mojo")
    protected void doExecute() throws Throwable {
        if (!MavenConfigUtils.isJarPackaging(this.project)) {
            throw new UnsupportedOperationException(
                String.format("The project (%s) with packaging %s is not supported for Azure Container Apps.", this.project.getName(),
                    this.project.getPackaging()));
        }
        if (isProjectConfigured(this.project)) {
            log.warn(String.format("Project (%s) is already configured and won't be affected by this command.", this.project.getName()));
            return;
        }
        final ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        try {
            this.wrapper = new ConfigurationPrompter(expressionEvaluator, "container-apps");
            this.wrapper.initialize();
            this.wrapper.putCommonVariable("project", this.project);

            // select subscription in container apps
            // set up account and select subscription here
            loginAzure();
            promptAndSelectSubscription();

            // prompt to select existing appEnv or create a new one
            useExistingAppEnv = this.wrapper.handleConfirm("Use existing Azure Container Apps Environment in Azure (Y/n):", true, true);
            ContainerAppsEnvironment appEnv = null;
            if (useExistingAppEnv) {
                appEnv = selectAppEnv();
            } else {
                configAppEnv();
            }
            final boolean useExistingApp = Objects.nonNull(appEnv) &&
                this.wrapper.handleConfirm(String.format("Use existing app in Azure Container App Environment %s (y/N):", appEnv.getName()), false, true);
            if (useExistingApp) {
                selectApp(appEnv);
            } else {
                configureAppName();
            }
            configCommon();
            confirmAndSave();
        } catch (IOException | InvalidConfigurationException | UnsupportedOperationException | MavenDecryptException | AzureToolkitAuthenticationException e) {
            throw new AzureToolkitRuntimeException(e.getMessage());
        } finally {
            if (this.wrapper != null) {
                try {
                    this.wrapper.close();
                } catch (IOException e) {
                    // ignore at final step
                }
            }

        }
    }

    private void configAppEnv() throws IOException, InvalidConfigurationException {
        configureAppEnvName();
        configureResourceGroup();
        configureRegion();
    }

    private void configureAppEnvName() throws IOException, InvalidConfigurationException {
        final String appEnvironmentName = this.wrapper.handle("configure-app-env-name", false);
        this.appEnvironmentName = appEnvironmentName;
        this.wrapper.putCommonVariable("appEnvironmentName", appEnvironmentName);
    }

    private void configureResourceGroup() throws IOException, InvalidConfigurationException {
        final String resourceGroup = this.wrapper.handle("configure-resource-group-name", false);
        this.resourceGroup = resourceGroup;
        this.wrapper.putCommonVariable("resourceGroup", resourceGroup);
    }

    private void configureRegion() throws IOException, InvalidConfigurationException {
        final List<Region> regions = Azure.az(AzureContainerApps.class)
            .forSubscription(getSubscriptionId()).listSupportedRegions("Container App");
        assert CollectionUtils.isNotEmpty(regions) : "No valid region found for Container App.";
        this.wrapper.putCommonVariable("regions", regions);
        final Region defaultRegion = regions.contains(Region.US_EAST) ? Region.US_EAST : regions.get(0);
        final Region result = this.wrapper.handleSelectOne("configure-region", regions, defaultRegion, Region::getName);
        this.region = result.getName();
    }

    private void configureAppName() throws IOException, InvalidConfigurationException {
        final String appName = this.wrapper.handle("configure-app-name", false);
        this.appName = appName;
        this.wrapper.putCommonVariable("appName", appName);
    }

    private void configCommon() throws IOException, InvalidConfigurationException {
        configureRegistry();
        configureIdentity();
        configureContainers();
        configureIngress();
        configureScale();
    }

    private void configureRegistry() throws IOException, InvalidConfigurationException {
        final boolean useExistingRegistry = this.wrapper.handleConfirm("Use existing Azure Container Registry (Y/n):", true, true);
        if (useExistingRegistry) {
            selectRegistry();
        } else {
            final String registryName = this.wrapper.handle("configure-registry-name", false);
            if (StringUtils.isNotBlank(registryName)) {
                this.registry = new ContainerRegistryConfig();
                this.registry.setRegistryName(registryName);
            }
        }
        this.wrapper.putCommonVariable("registry", this.registry);
    }

    private void selectRegistry() throws IOException, InvalidConfigurationException {
        log.info("It may take a few minutes to list Azure Container Registries in your account, please be patient.");
        final AzureContainerRegistryModule module = Azure.az(AzureContainerRegistry.class).registry(subscriptionId);
        final List<ContainerRegistry> registries = module.list();
        this.wrapper.putCommonVariable("registries", registries);
        final ContainerRegistry targetRegistry = this.wrapper.handleSelectOne("select-registry", registries, null, AbstractAzResource::getName);
        if (targetRegistry != null) {
            log.info(String.format("Using Azure Container Registry: %s", TextUtils.blue(targetRegistry.getName())));
        }
        this.registry = new ContainerRegistryConfig();
        this.registry.setRegistryName(targetRegistry.getName());
        this.registry.setResourceGroup(targetRegistry.getResourceGroup().getName());
    }

    private void configureIdentity() throws IOException, InvalidConfigurationException {
        final boolean useExistingIdentity = this.wrapper.handleConfirm("Use existing User Assigned Identity (y/N):", false, true);
        if (useExistingIdentity) {
            log.info("It may take a few minutes to list User Assigned Identities in your account, please be patient.");
            final List<Identity> identities = Azure.az(AzureManagedIdentity.class).identities();
            this.wrapper.putCommonVariable("identities", identities);
            final Identity identity = this.wrapper.handleSelectOne("select-identity", identities, null, AbstractAzResource::getName);
            if (identity != null) {
                log.info(String.format("Using User Assigned Identity: %s", TextUtils.blue(identity.getName())));
            }
            this.identity = identity.getId();
        }
    }

    private void configureContainers() throws IOException, InvalidConfigurationException {
        //final String deploymentType = this.wrapper.handle("configure-deployment-type", false);
        List<String> deploymentTypes = Arrays.asList(DeploymentType.CODE.name(), DeploymentType.ARTIFACT.name(), DeploymentType.IMAGE.name());
        final String deploymentType = this.wrapper.handleSelectOne("select-deployment-type", deploymentTypes, null, String::toString);
        AppContainerMavenConfig container = new AppContainerMavenConfig();
        container.setType(deploymentType);
        this.containers = Collections.singletonList(container);
        this.wrapper.putCommonVariable("container", container);
        if (container.getDeploymentType() == DeploymentType.IMAGE) {
            container.setImage(this.wrapper.handle("configure-image", false));
        }
    }

    private void configureIngress() throws IOException, InvalidConfigurationException {

        final boolean ingressEnabled = this.wrapper.handleConfirm("Configure ingress (Y/n):", true, true);
        if (ingressEnabled) {
            this.ingress = new IngressMavenConfig();
            this.ingress.setExternal(true);
            final String ingressPort = this.wrapper.handle("configure-ingress-port", false);
            this.ingress.setTargetPort(Integer.valueOf(ingressPort));
            this.wrapper.putCommonVariable("ingress", this.ingress);
        }
    }

    private void configureScale() throws IOException, InvalidConfigurationException {
        final boolean scaleEnabled = this.wrapper.handleConfirm("Configure scale (y/N):", false, true);
        if (scaleEnabled) {
            final String minReplicas = this.wrapper.handle("configure-min-replicas", false);
            final String maxReplicas = this.wrapper.handle("configure-max-replicas", false);
            this.scale = ContainerAppDraft.ScaleConfig.builder().minReplicas(Integer.valueOf(minReplicas)).maxReplicas(Integer.valueOf(maxReplicas)).build();
            this.wrapper.putCommonVariable("scale", this.scale);
        }
    }

    private ContainerAppsEnvironment selectAppEnv() throws IOException, InvalidConfigurationException {
        log.info("It may take a few minutes to list Azure Container Apps Environments in your account, please be patient.");
        final ContainerAppsEnvironmentModule module = Azure.az(AzureContainerApps.class).environments(subscriptionId);
        if (StringUtils.isNotBlank(appEnvironmentName)) {
            final ContainerAppsEnvironment appEnvironment = module.get(this.appEnvironmentName, this.resourceGroup);
            if (Objects.nonNull(appEnvironment) && appEnvironment.exists()) {
                return appEnvironment;
            }
            log.warn(String.format("Cannot find Azure Container App Environment with name: %s in resource group: %s.",
                TextUtils.yellow(this.appEnvironmentName), TextUtils.yellow(this.resourceGroup)));
        }
        final List<ContainerAppsEnvironment> appsEnvironments = module.list();
        this.wrapper.putCommonVariable("appsEnvironments", appsEnvironments);
        final ContainerAppsEnvironment targetAppEnv = this.wrapper.handleSelectOne("select-container-apps-env", appsEnvironments, null, AbstractAzResource::getName);
        if (targetAppEnv != null) {
            log.info(String.format("Using Azure Container Apps: %s", TextUtils.blue(targetAppEnv.getName())));
        }
        this.resourceGroup = targetAppEnv.getResourceGroup().getName();
        this.appEnvironmentName = targetAppEnv.getName();
        this.region = targetAppEnv.getRegion().getName();
        return targetAppEnv;
    }

    private void selectApp(ContainerAppsEnvironment appEnv) throws IOException, InvalidConfigurationException {
        final List<ContainerApp> apps = appEnv.listContainerApps();
        this.wrapper.putCommonVariable("apps", apps);
        final ContainerApp app = this.wrapper.handleSelectOne("select-app", apps, null, AbstractAzResource::getName);
        if (app != null) {
            log.info(String.format("Using Azure Container App: %s", TextUtils.blue(app.getName())));
        }
        this.appName = app.getName();
    }

    private void confirmAndSave() throws IOException {
        final Map<String, String> changesToConfirm = new LinkedHashMap<>();
        changesToConfirm.put("Subscription id", this.subscriptionId);
        changesToConfirm.put("Resource group name", this.resourceGroup);
        changesToConfirm.put("Azure Container App Environment name", this.appEnvironmentName);
        changesToConfirm.put("Region", this.region);
        changesToConfirm.put("App name", this.appName);
        if (StringUtils.isNotBlank(identity)) {
            changesToConfirm.put("Identity", identity);
        }
        if (Objects.nonNull(registry)) {
            changesToConfirm.put("Registry name", registry.getRegistryName());
        }
        if (Objects.nonNull(containers)) {
            changesToConfirm.put("Deployment type", containers.get(0).getType());
            if (DeploymentType.CODE.equals(containers.get(0).getDeploymentType())
                || DeploymentType.ARTIFACT.equals(containers.get(0).getDeploymentType())) {
                changesToConfirm.put("Directory", containers.get(0).getDirectory());
            } else {
                changesToConfirm.put("Image", containers.get(0).getImage());
            }
        }
        if (Objects.nonNull(ingress.getExternal())) {
            changesToConfirm.put("Ingress external", String.valueOf(ingress.getExternal()));
            changesToConfirm.put("Ingress target port", String.valueOf(ingress.getTargetPort()));
        }
        if (Objects.nonNull(scale.getMaxReplicas())) {
            changesToConfirm.put("Min replicas", String.valueOf(scale.getMinReplicas()));
            changesToConfirm.put("Max replicas", String.valueOf(scale.getMaxReplicas()));
        }

        this.wrapper.confirmChanges(changesToConfirm, this::saveConfigurationToPom);
    }

    private Integer saveConfigurationToPom() {
        telemetries.put(TELEMETRY_KEY_POM_FILE_MODIFIED, String.valueOf(true));
        try {
            saveConfigurationToProject();
        } catch (DocumentException | IOException e) {
            throw Lombok.sneakyThrow(e);
        }
        return 1;
    }

    public void saveConfigurationToProject() throws DocumentException, IOException {
        final File pom = project.getFile();
        final Element pluginNode = PomUtils.getPluginNode(plugin, pom);
        Element configNode = createOrUpdateAppConfigNode(pluginNode);
        // newly created nodes are not LocationAwareElement
        while (!(configNode.getParent() instanceof PomUtils.LocationAwareElement)) {
            configNode = configNode.getParent();
        }
        FileUtils.fileWrite(pom, PomUtils.formatNode(FileUtils.fileRead(pom), (PomUtils.LocationAwareElement) configNode.getParent(), configNode));

    }

    private Element createOrUpdateAppConfigNode(Element pluginNode) {
        final Element appConfigNode = PomUtils.getOrCreateNode(pluginNode, "configuration");
        PomUtils.updateNode(appConfigNode, configtoMap());
        if (Objects.nonNull(containers)) {
            final Element containersNode = PomUtils.getOrCreateNode(appConfigNode, "containers");
            final Element containerNode = PomUtils.getOrCreateNode(containersNode, "container");
            PomUtils.updateNode(containerNode, containerToMap());
        }
        if (Objects.nonNull(registry) && StringUtils.isNotBlank(registry.getRegistryName())) {
            final Element registryNode = PomUtils.getOrCreateNode(appConfigNode, "registry");
            Map<String, Object> registryMap = MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[]{
                new DefaultMapEntry<>("registryName", registry.getRegistryName())
            });
            if (StringUtils.isNotBlank(registry.getResourceGroup())) {
                registryMap.put("resourceGroup", registry.getResourceGroup());
            }
            PomUtils.updateNode(registryNode, registryMap);
        }
        if (Objects.nonNull(ingress.getExternal())) {
            final Element ingressNode = PomUtils.getOrCreateNode(appConfigNode, "ingress");
            PomUtils.updateNode(ingressNode, MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[]{
                new DefaultMapEntry<>("targetPort", ingress.getTargetPort()),
                new DefaultMapEntry<>("external", ingress.getExternal())
            }));
        }
        if (Objects.nonNull(scale.getMaxReplicas())) {
            final Element scaleNode = PomUtils.getOrCreateNode(appConfigNode, "scale");
            PomUtils.updateNode(scaleNode, MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[]{
                new DefaultMapEntry<>("minReplicas", scale.getMinReplicas()),
                new DefaultMapEntry<>("maxReplicas", scale.getMaxReplicas())
            }));
        }
        return appConfigNode;
    }

    private Map<String, Object> configtoMap() {
        Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[]{
            new DefaultMapEntry<>("subscriptionId", subscriptionId),
            new DefaultMapEntry<>("resourceGroup", resourceGroup),
            new DefaultMapEntry<>("appEnvironmentName", appEnvironmentName),
            new DefaultMapEntry<>("appName", appName),
        });
        if (StringUtils.isNotBlank(region)) {
            map.put("region", region);
        }
        if (StringUtils.isNotBlank(identity)) {
            map.put("identity", identity);
        }
        return map;
    }

    private Map<String, Object> containerToMap() {
        Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[]{
            new DefaultMapEntry<>("type", containers.get(0).getType()),
        });
        if (DeploymentType.CODE.equals(containers.get(0).getDeploymentType())
            || DeploymentType.ARTIFACT.equals(containers.get(0).getDeploymentType())) {
            map.put("directory", containers.get(0).getDirectory());
        } else {
            map.put("image", containers.get(0).getImage());
        }
        return map;
    }

    @SneakyThrows
    protected void promptAndSelectSubscription() {
        if (StringUtils.isBlank(subscriptionId)) {
            final List<Subscription> subscriptions = Azure.az(AzureAccount.class).account().getSubscriptions();
            subscriptionId = (CollectionUtils.isNotEmpty(subscriptions) && subscriptions.size() == 1) ? subscriptions.get(0).getId() : promptSubscription();
        }
        // use selectSubscription to set selected subscriptions in account and print current subscription
        selectSubscription();
    }

    private String promptSubscription() throws IOException, InvalidConfigurationException {
        final List<Subscription> subscriptions = Azure.az(AzureAccount.class).account().getSubscriptions();
        final List<Subscription> selectedSubscriptions = Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
        this.wrapper.putCommonVariable("subscriptions", subscriptions);
        final Subscription select = this.wrapper.handleSelectOne("select-subscriptions", subscriptions,
            CollectionUtils.isNotEmpty(selectedSubscriptions) ? selectedSubscriptions.get(0) : null,
            t -> String.format("%s (%s)", t.getName(), t.getId()));
        com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account().setSelectedSubscriptions(Collections.singletonList(select.getId()));
        return select.getId();
    }

    private boolean isProjectConfigured(MavenProject proj) {
        final String pluginIdentifier = plugin.getPluginLookupKey();
        final Xpp3Dom configuration = MavenConfigUtils.getPluginConfiguration(proj, pluginIdentifier);
        if (configuration == null) {
            return false;
        }
        int count = 0;
        for (final Xpp3Dom child : configuration.getChildren()) {
            if (REQUIRED_PROPERTIES.contains(child.getName())) {
                count++;
            }
        }
        return count == REQUIRED_PROPERTIES.size();
    }
}
