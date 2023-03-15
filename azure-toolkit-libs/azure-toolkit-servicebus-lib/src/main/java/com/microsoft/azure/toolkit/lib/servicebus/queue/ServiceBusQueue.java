package com.microsoft.azure.toolkit.lib.servicebus.queue;

import com.azure.resourcemanager.servicebus.ServiceBusManager;
import com.azure.resourcemanager.servicebus.fluent.ServiceBusManagementClient;
import com.azure.resourcemanager.servicebus.fluent.models.SBAuthorizationRuleInner;
import com.azure.resourcemanager.servicebus.fluent.models.SBQueueInner;
import com.azure.resourcemanager.servicebus.models.*;
import com.azure.resourcemanager.servicebus.models.Queue;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.servicebus.ServiceBusNamespace;
import com.microsoft.azure.toolkit.lib.servicebus.model.ServiceBusInstance;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ServiceBusQueue extends AbstractAzResource<ServiceBusQueue, ServiceBusNamespace, Queue> implements ServiceBusInstance {
    @Nullable
    private EntityStatus entityStatus;
    protected ServiceBusQueue(@Nonnull String name, @Nonnull ServiceBusQueueModule module) {
        super(name, module);
    }

    protected ServiceBusQueue(@Nonnull Queue remote, @Nonnull ServiceBusQueueModule module) {
        super(remote.name(), module);
    }

    @Override
    protected void updateAdditionalProperties(@Nullable Queue newRemote, @Nullable Queue oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        this.entityStatus = Optional.ofNullable(newRemote).map(Queue::innerModel).map(SBQueueInner::status).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull Queue remote) {
        return remote.innerModel().status().toString();
    }

    @Override
    public String getOrCreateListenConnectionString() {
        return getOrCreateConnectionString(Collections.singletonList(AccessRights.LISTEN));
    }

    @Override
    public void updateStatus(EntityStatus status) {
        final SBQueueInner inner = remoteOptional().map(Queue::innerModel).orElse(new SBQueueInner());
        final ServiceBusNamespace namespace = this.getParent();
        Optional.ofNullable(namespace.getParent().getRemote())
                .map(ServiceBusManager::serviceClient)
                .map(ServiceBusManagementClient::getQueues)
                .ifPresent(c -> doModify(() -> c.createOrUpdate(getResourceGroupName(), namespace.getName(), getName(), inner.withStatus(status)), Status.UPDATING));

    }

    @Override
    @Nullable
    public EntityStatus getEntityStatus() {
        return this.entityStatus;
    }

    private String getOrCreateConnectionString(List<AccessRights> accessRights) {
        final List<QueueAuthorizationRule> connectionStrings = Optional.ofNullable(getRemote())
                .map(queue -> queue.authorizationRules().list().stream()
                        .filter(rule -> new HashSet<>(rule.rights()).containsAll(accessRights))
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
        if (connectionStrings.size() > 0) {
            return connectionStrings.get(0).getKeys().primaryConnectionString();
        }
        final ServiceBusManager manager = getParent().getParent().getRemote();
        if (Objects.isNull(manager)) {
            throw new AzureToolkitRuntimeException(AzureString.format("resource ({0}) not found", getName()).toString());
        }
        final String accessRightsStr = StringUtils.join(accessRights, "-");
        manager.serviceClient().getQueues().createOrUpdateAuthorizationRule(getResourceGroupName(), getParent().getName(),
                getName(), accessRightsStr, new SBAuthorizationRuleInner().withRights(accessRights));
        return manager.serviceClient().getQueues().listKeys(getResourceGroupName(), getParent().getName(),
                getName(), accessRightsStr).primaryConnectionString();
    }
}
