/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public abstract class OperationBase implements Operation {
    @Getter
    @Setter
    private Operation parent;
    private OperationContext context;
    private Id idObject;

    @Nonnull
    public String getExecutionId() {
        return Utils.getId(this);
    }

    @Override
    public synchronized OperationContext getContext() {
        if (Objects.isNull(this.context)) {
            this.context = new OperationContext(this);
        }
        return this.context;
    }

    private synchronized Id getIdObject() {
        if (Objects.isNull(this.idObject)) {
            this.idObject = new Id(this.getId());
        }
        return this.idObject;
    }

    @Nonnull
    @Override
    public String getType() {
        return this.getIdObject().getType();
    }

    @Nonnull
    @Override
    public final String getServiceName() {
        final String serviceName = this.getIdObject().getService();
        if (serviceName.contains("$")) {
            final Object source = this.getSource();
            if (source instanceof AzResourceModule) {
                return ((AzResourceModule<?>) source).getServiceNameForTelemetry();
            } else if (source instanceof AzResource) {
                return ((AzResource) source).getModule().getServiceNameForTelemetry();
            } else {
                String serviceNameFromContext = this.context.getProperty("serviceName");
                if (StringUtils.isBlank(serviceNameFromContext)) {
                    serviceNameFromContext = Optional.ofNullable(this.getActionParent()).map(Operation::getServiceName).orElse(null);
                }
                if (StringUtils.isNotBlank(serviceNameFromContext)) {
                    return serviceNameFromContext;
                }
            }
        }
        return serviceName;
    }

    @Nonnull
    @Override
    public final String getOperationName() {
        return this.getIdObject().getOperation();
    }

    private static class Id {
        private final String id;
        @Getter
        private String type;
        @Getter
        private String service;
        @Getter
        private String operation;

        public Id(String id) {
            this.id = id;
            try {
                final String[] parts = id.split("\\."); // ["internal/appservice", "list_file", "dir"]
                final String[] typeAndService = parts[0].split("/"); // ["internal", "appservice"]
                this.type = typeAndService[0];
                this.service = typeAndService[1];
                this.operation = parts[1];
            } catch (final Exception e) {
                this.type = "unknown";
                this.service = "unknown";
                this.operation = "unknown";
            }
        }

        @Override
        public String toString() {
            return this.id;
        }
    }
}
