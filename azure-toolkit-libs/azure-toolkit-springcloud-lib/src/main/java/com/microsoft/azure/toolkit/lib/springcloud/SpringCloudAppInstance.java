package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.azure.resourcemanager.appplatform.models.DeploymentInstance;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.utils.StreamingLogSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudAppInstance extends AbstractAzResource<SpringCloudAppInstance, SpringCloudDeployment, DeploymentInstance>
    implements StreamingLogSupport {
    private static final String REMOTE_URL_TEMPLATE = "https://%s/api/remoteDebugging/apps/%s/deployments/%s/instances/%s?port=%s";

    protected SpringCloudAppInstance(@Nonnull String name, @Nonnull SpringCloudAppInstanceModule module) {
        super(name, module);
    }

    protected SpringCloudAppInstance(@Nonnull DeploymentInstance remote, @Nonnull SpringCloudAppInstanceModule module) {
        super(remote.name(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull DeploymentInstance remote) {
        return remote.status();
    }

    public String getDiscoveryStatus() {
        final DeploymentInstance deploymentInstance = this.getRemote();
        if (Objects.nonNull(deploymentInstance)) {
            return deploymentInstance.discoveryStatus();
        }
        return Status.UNKNOWN;
    }

    public String getRemoteDebuggingUrl() {
        SpringCloudApp app = this.getParent().getParent();
        return String.format(REMOTE_URL_TEMPLATE, app.getParent().getFqdn(), app.getName(), this.getParent().getName(), this.getName(), this.getParent().getRemoteDebuggingPort());
    }

    @Nullable
    public String getLogStreamEndpoint() {
        final SpringCloudApp app = this.getParent().getParent();
        final SpringCloudCluster service = app.getParent();
        if (service.isConsumptionTier()) {
            return String.format("https://%s/proxy/logstream%s?follow=true&tailLines=300&tenantId=%s", service.getFqdn(), this.getId(), this.getSubscription().getTenantId());
        } else {
            return Optional.ofNullable(app.getTestEndpoint())
                .map(e -> String.format("%s/api/logstream/apps/%s/instances/%s", e.replace(".test", ""), app.getName(), this.getName()))
                .orElse(null);
        }
    }

    @Override
    public String getLogStreamAuthorization() {
        final SpringCloudApp app = this.getParent().getParent();
        final SpringCloudCluster service = app.getParent();
        if (service.isConsumptionTier()) {
            Account account = Azure.az(AzureAccount.class).account();
            String[] scopes = ScopeUtil.resourceToScopes(account.getEnvironment().getManagementEndpoint());
            TokenRequestContext request = (new TokenRequestContext()).addScopes(scopes);
            String accessToken = Objects.requireNonNull(account.getTokenCredential(this.getSubscriptionId()).getToken(request).block()).getToken();
            return "Bearer " + accessToken;
        } else {
            final String password = service.getTestKey();
            final String userPass = "primary:" + password;
            return "Basic " + new String(Base64.getEncoder().encode(userPass.getBytes()));
        }
    }
}
