package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.appplatform.models.DeploymentInstance;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SpringCloudAppInstanceModule extends AbstractAzResourceModule<SpringCloudAppInstance, SpringCloudDeployment, DeploymentInstance> {
    public static final String NAME = "instances";

    public SpringCloudAppInstanceModule(@NotNull SpringCloudDeployment parent) {
        super(NAME, parent);
    }

    @Nullable
    @Override
    protected List<DeploymentInstance> getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(SpringAppDeployment::instances).orElse(null);
    }

    @Nullable
    @Override
    protected DeploymentInstance loadResourceFromAzure(@NotNull String name, @Nullable String resourceGroup) {
        List<DeploymentInstance> deploymentInstanceList = Optional.ofNullable(this.getClient()).orElse(Collections.emptyList());
        return deploymentInstanceList.stream().filter(instance -> name.equals(instance.name())).findAny().orElse(null);
    }

    @Override
    protected Iterator<? extends ContinuablePage<String, DeploymentInstance>> loadResourcePagesFromAzure() {
        return Collections.singletonList(new ItemPage<>(this.loadResourcesFromAzure())).iterator();
    }

    @NotNull
    @Override
    protected Stream<DeploymentInstance> loadResourcesFromAzure() {
        List<DeploymentInstance> deploymentInstanceList = Optional.ofNullable(this.getClient()).orElse(Collections.emptyList());
        return deploymentInstanceList.stream();
    }

    @NotNull
    @Override
    protected SpringCloudAppInstance newResource(@NotNull DeploymentInstance deploymentInstance) {
        return new SpringCloudAppInstance(deploymentInstance, this);
    }

    @NotNull
    @Override
    protected SpringCloudAppInstance newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new SpringCloudAppInstance(name, this);
    }

    @NotNull
    @Override
    public String getResourceTypeName() {
        return "Spring App instance";
    }
}
