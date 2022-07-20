package com.microsoft.azure.toolkit.lib.containerservice;

import com.azure.resourcemanager.containerservice.ContainerServiceManager;
import com.azure.resourcemanager.containerservice.models.AgentPoolMode;
import com.azure.resourcemanager.containerservice.models.ContainerServiceVMSizeTypes;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster.DefinitionStages;
import com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool;
import com.azure.resourcemanager.containerservice.models.LoadBalancerSku;
import com.azure.resourcemanager.containerservice.models.NetworkPlugin;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerservice.model.VirtualMachineSize;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class KubernetesClusterDraft extends KubernetesCluster implements
        AzResource.Draft<KubernetesCluster, com.azure.resourcemanager.containerservice.models.KubernetesCluster> {

    public static final String AGENTPOOL = "agentpool";
    @Getter
    @Nullable
    private final KubernetesCluster origin;

    @Nullable
    @Getter
    @Setter
    private Config config;

    protected KubernetesClusterDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull KubernetesClusterModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    protected KubernetesClusterDraft(@Nonnull KubernetesCluster origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    public com.azure.resourcemanager.containerservice.models.KubernetesCluster createResourceInAzure() {
        final Region region = Objects.requireNonNull(getRegion(), "'region' is required to create Azure Kubernetes Service");
        final String dnsPrefix = Objects.requireNonNull(getDnsPrefix(), "'dnsPrefix' is required to create Azure Kubernetes Service");
        final String kubernetesVersion = Objects.requireNonNull(getKubernetesVersion(), "'dnsPrefix' is required to create Azure Kubernetes Service");
        final Integer vmCount = getVmCount();
        final Integer maxVMCount = getMaxVMCount();
        final Integer minVMCount = getMinVMCount();
        final VirtualMachineSize size = Objects.requireNonNull(getVirtualMachineSize(), "'VirtualMachineSize' is required to create Azure Kubernetes Service");
        // Get or create resource group
        final ResourceGroup resourceGroup =
                Azure.az(AzureResources.class).groups(getSubscriptionId()).getOrDraft(getResourceGroupName(), getResourceGroupName());
        if (resourceGroup.isDraftForCreating()) {
            ((ResourceGroupDraft) resourceGroup).setRegion(region);
            ((ResourceGroupDraft) resourceGroup).createIfNotExist();
        }
        // Create Kubernetes service
        final ContainerServiceManager manager = this.getParent().getRemote();
        final DefinitionStages.WithCreate withCreate = manager.kubernetesClusters().define(this.getName())
                .withRegion(region.getName())
                .withExistingResourceGroup(this.getResourceGroupName())
                .withVersion(kubernetesVersion)
                .withSystemAssignedManagedServiceIdentity();
        // Define agent pool
        final KubernetesClusterAgentPool.DefinitionStages.WithAttach<? extends DefinitionStages.WithCreate> withAttach = withCreate
                .defineAgentPool(AGENTPOOL)
                .withVirtualMachineSize(ContainerServiceVMSizeTypes.fromString(size.getValue()))
                .withAgentPoolVirtualMachineCount(vmCount);
        if (ObjectUtils.allNotNull(minVMCount, maxVMCount)) {
            withAttach.withAutoScaling(minVMCount, maxVMCount);
        }
        withAttach.withAgentPoolMode(AgentPoolMode.SYSTEM).attach();
        // Define network profile
        withCreate.defineNetworkProfile().withNetworkPlugin(NetworkPlugin.KUBENET).withLoadBalancerSku(LoadBalancerSku.BASIC).attach();
        // Define dns prefix
        withCreate.withDnsPrefix(dnsPrefix);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Kubernetes service ({0})...", getName()));
        com.azure.resourcemanager.containerservice.models.KubernetesCluster cluster = (com.azure.resourcemanager.containerservice.models.KubernetesCluster)
                Objects.requireNonNull(this.doModify(() -> withCreate.create(), Status.CREATING));
        messager.success(AzureString.format("Kubernetes service ({0}) is successfully created", getName()));
        return cluster;
    }

    @Nonnull
    @Override
    public com.azure.resourcemanager.containerservice.models.KubernetesCluster updateResourceInAzure(
            @Nonnull com.azure.resourcemanager.containerservice.models.KubernetesCluster origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return config != null && !Objects.equals(config, new Config());
    }

    @Nullable
    @Override
    public KubernetesCluster getOrigin() {
        return this.origin;
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    public Region getRegion() {
        return Optional.ofNullable(config).map(Config::getRegion).orElse(null);
    }

    public void setRegion(Region region) {
        this.ensureConfig().setRegion(region);
    }

    public String getKubernetesVersion() {
        return Optional.ofNullable(config).map(Config::getKubernetesVersion).orElse(null);
    }

    public void setKubernetesVersion(String kubernetesVersion) {
        this.ensureConfig().setKubernetesVersion(kubernetesVersion);
    }

    public String getDnsPrefix() {
        return Optional.ofNullable(config).map(Config::getDnsPrefix).orElse(null);
    }

    public void setDnsPrefix(String dnsPrefix) {
        this.ensureConfig().setDnsPrefix(dnsPrefix);
    }

    public Integer getVmCount() {
        return Optional.ofNullable(config).map(Config::getVmCount).orElse(null);
    }

    public void setVmCount(Integer vmCount) {
        this.ensureConfig().setVmCount(vmCount);
    }

    public Integer getMinVMCount() {
        return Optional.ofNullable(config).map(Config::getMinVMCount).orElse(null);
    }

    public void setMinVMCount(Integer minVMCount) {
        this.ensureConfig().setMinVMCount(minVMCount);
    }

    public Integer getMaxVMCount() {
        return Optional.ofNullable(config).map(Config::getMaxVMCount).orElse(null);
    }

    public void setMaxVMCount(Integer maxVMCount) {
        this.ensureConfig().setMaxVMCount(maxVMCount);
    }

    public VirtualMachineSize getVirtualMachineSize() {
        return Optional.ofNullable(config).map(Config::getSize).orElse(null);
    }

    public void setVirtualMachineSize(VirtualMachineSize size) {
        this.ensureConfig().setSize(size);
    }

    @Data
    @EqualsAndHashCode
    public static class Config {
        private Subscription subscription;
        private String name;
        private ResourceGroup resourceGroup;
        private Region region;
        private String kubernetesVersion;
        private String dnsPrefix;
        private Integer vmCount;
        private Integer minVMCount;
        private Integer maxVMCount;
        private VirtualMachineSize size;
    }
}
