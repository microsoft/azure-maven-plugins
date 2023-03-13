package com.microsoft.azure.toolkit.lib.servicebus;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.servicebus.ServiceBusManager;
import com.azure.resourcemanager.servicebus.models.ServiceBusNamespaces;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public class ServiceBusNamespaceModule extends AbstractAzResourceModule<ServiceBusNamespace, ServiceBusNamespaceSubscription, com.azure.resourcemanager.servicebus.models.ServiceBusNamespace> {

    public static final String NAME = "namespaces";
    public ServiceBusNamespaceModule(ServiceBusNamespaceSubscription parent) {
        super(NAME, parent);
    }

    @NotNull
    @Override
    protected Iterator<? extends ContinuablePage<String, com.azure.resourcemanager.servicebus.models.ServiceBusNamespace>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().iterableByPage(getPageSize()).iterator()).orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"})
    protected com.azure.resourcemanager.servicebus.models.ServiceBusNamespace loadResourceFromAzure(@NotNull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(serviceBusNamespaces -> serviceBusNamespaces.getByResourceGroup(resourceGroup, name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/servicebus.delete_service_bus_namespace.servicebus", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@NotNull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(serviceBusNamespaces -> serviceBusNamespaces.deleteById(resourceId));
    }

    @NotNull
    @Override
    protected ServiceBusNamespace newResource(@NotNull com.azure.resourcemanager.servicebus.models.ServiceBusNamespace remote) {
        return new ServiceBusNamespace(remote, this);
    }

    @NotNull
    @Override
    protected ServiceBusNamespace newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return null;
    }

    @Nullable
    @Override
    protected ServiceBusNamespaces getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(ServiceBusManager::namespaces).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Service Bus Namespace";
    }
}
