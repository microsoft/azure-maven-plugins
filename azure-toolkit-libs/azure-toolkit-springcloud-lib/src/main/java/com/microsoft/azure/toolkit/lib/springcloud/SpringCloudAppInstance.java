package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.DeploymentInstance;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SpringCloudAppInstance extends AbstractAzResource<SpringCloudAppInstance, SpringCloudDeployment, DeploymentInstance> {
    private static final String REMOTE_URL_TEMPLATE = "https://%s/api/remoteDebugging/apps/%s/deployments/%s/instances/%s";

    protected SpringCloudAppInstance(@NotNull String name, @NotNull SpringCloudAppInstanceModule module) {
        super(name, module);
    }

    protected SpringCloudAppInstance(@Nonnull DeploymentInstance remote, @Nonnull SpringCloudAppInstanceModule module) {
        super(remote.name(), module);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, SpringCloudAppInstance, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull DeploymentInstance remote) {
        return remote.status();
    }

    public String getDiscoveryStatus() {
        final DeploymentInstance deploymentInstance = this.getRemote();
        if (Objects.nonNull(deploymentInstance)) {
            return deploymentInstance.discoveryStatus();
        }
        return Status.UNKNOWN;
    }

    public String getRemoteUrl() {
        SpringCloudApp app = this.getParent().getParent();
        return String.format(REMOTE_URL_TEMPLATE, app.getParent().getFqdn(), app.getName(), this.getParent().getName(), this.getName());
    }

}
