/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.deploy.FTPFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.IFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.MSFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.RunFromBlobFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.RunFromZipFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.ZIPFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.file.AzureFunctionsResourceManager;
import com.microsoft.azure.toolkit.lib.appservice.file.IFileClient;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;

public abstract class FunctionAppBase<T extends FunctionAppBase<T, P, R>, P extends AbstractAzResource<P, ?, ?>, R extends WebAppBase>
    extends AppServiceAppBase<T, P, R> {

    private AzureFunctionsResourceManager fileClient;

    protected FunctionAppBase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        super(name, resourceGroupName, module);
    }

    protected FunctionAppBase(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        super(name, module);
    }

    protected FunctionAppBase(@Nonnull T origin) {
        super(origin);
    }

    public void deploy(File targetFile) {
        deploy(targetFile, getDefaultDeployType());
    }

    public void deploy(File targetFile, FunctionDeployType functionDeployType) {
        getDeployHandlerByType(functionDeployType).deploy(targetFile, getRemote());
    }

    @Nullable
    protected IFileClient getFileClient() {
        // kudu api does not applies to linux consumption, using functions admin api instead
        if (fileClient == null) {
            fileClient = this.remoteOptional().map(r -> AzureFunctionsResourceManager.getClient(r, this)).orElse(null);
        }
        return fileClient;
    }

    protected FunctionDeployType getDefaultDeployType() {
        final PricingTier pricingTier = Optional.ofNullable(getAppServicePlan()).map(AppServicePlan::getPricingTier).orElse(PricingTier.PREMIUM_P1V2);
        final OperatingSystem os = Optional.ofNullable(getRuntime()).map(Runtime::getOperatingSystem).orElse(OperatingSystem.LINUX);
        if (os == OperatingSystem.WINDOWS) {
            return FunctionDeployType.RUN_FROM_ZIP;
        }
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

    public abstract String getMasterKey();
}
