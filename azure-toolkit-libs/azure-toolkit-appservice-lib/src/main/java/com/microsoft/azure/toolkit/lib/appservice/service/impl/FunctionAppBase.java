/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServiceBaseEntity;
import com.microsoft.azure.toolkit.lib.appservice.manager.AzureFunctionsResourceManager;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.service.IFileClient;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy.FTPFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy.IFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy.MSFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy.RunFromBlobFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy.RunFromZipFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy.ZIPFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;

public abstract class FunctionAppBase<T extends WebAppBase, R extends AppServiceBaseEntity> extends AbstractAppService<T, R> implements IFunctionAppBase<R> {
    private AzureFunctionsResourceManager functionsResourceManager;

    public FunctionAppBase(@Nonnull String id) {
        super(id);
    }

    public FunctionAppBase(@Nonnull String subscriptionId, @Nonnull String resourceGroup, @Nonnull String name) {
        super(subscriptionId, resourceGroup, name);
    }

    public FunctionAppBase(@Nonnull WebSiteBase webSiteBase) {
        super(webSiteBase);
    }

    @Override
    public void deploy(File targetFile) {
        deploy(targetFile, getDefaultDeployType());
    }

    @Override
    public void deploy(File targetFile, FunctionDeployType functionDeployType) {
        getDeployHandlerByType(functionDeployType).deploy(targetFile, remote());
    }

    @Override
    protected IFileClient getFileClient() {
        // kudu api does not applies to linux consumption, using functions admin api instead
        if (functionsResourceManager == null) {
            functionsResourceManager = AzureFunctionsResourceManager.getClient(remote(), this);
        }
        return functionsResourceManager;
    }

    protected FunctionDeployType getDefaultDeployType() {
        if (getRuntime().getOperatingSystem() == OperatingSystem.WINDOWS) {
            return FunctionDeployType.RUN_FROM_ZIP;
        }
        final PricingTier pricingTier = getAppServicePlan().getPricingTier();
        return StringUtils.equalsAnyIgnoreCase(pricingTier.getTier(), "Dynamic", "ElasticPremium") ?
                FunctionDeployType.RUN_FROM_BLOB : FunctionDeployType.RUN_FROM_ZIP;
    }

    protected IFunctionDeployHandler getDeployHandlerByType(final FunctionDeployType deployType) {
        switch (deployType) {
            case FTP:
                return new FTPFunctionDeployHandler();
            case ZIP:
                return new ZIPFunctionDeployHandler();
            case MSDEPLOY:
                return new MSFunctionDeployHandler();
            case RUN_FROM_ZIP:
                return new RunFromZipFunctionDeployHandler();
            case RUN_FROM_BLOB:
                return new RunFromBlobFunctionDeployHandler();
            default:
                throw new AzureToolkitRuntimeException("Unsupported deployment type");
        }
    }
}
