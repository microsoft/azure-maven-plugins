package com.microsoft.azure.toolkit.lib.servicebus.model;

import com.azure.resourcemanager.servicebus.models.EntityStatus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nullable;

public interface ServiceBusInstance extends AzResource {
    String getOrCreateListenConnectionString();
    void updateStatus(EntityStatus status);
    @Nullable EntityStatus getEntityStatus();

}
