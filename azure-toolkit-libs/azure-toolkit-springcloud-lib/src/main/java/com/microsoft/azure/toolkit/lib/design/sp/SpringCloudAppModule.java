/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.design.sp;


import com.azure.resourcemanager.appplatform.models.PersistentDisk;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringApps;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.design.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

public class SpringCloudAppModule extends AbstractAzResourceModule<SpringCloudApp, SpringCloudCluster, SpringApp> {
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    public static final String DEFAULT_DISK_MOUNT_PATH = "/persistent";
    /**
     * @see <a href="https://azure.microsoft.com/en-us/pricing/details/spring-cloud/">Pricing - Azure Spring Cloud</a>
     */
    public static final int BASIC_TIER_DEFAULT_DISK_SIZE = 1;
    /**
     * @see <a href="https://azure.microsoft.com/en-us/pricing/details/spring-cloud/">Pricing - Azure Spring Cloud</a>
     */
    public static final int STANDARD_TIER_DEFAULT_DISK_SIZE = 50;

    public static final String NAME = "apps";

    public SpringCloudAppModule(@Nonnull SpringCloudCluster parent) {
        super(NAME, parent);
    }

    @Override
    public SpringApps getClient() {
        return this.parent.getRemote().apps();
    }

    @Override
    protected SpringApp createResourceInAzure(String appName, String resourceGroup, Object config) {
        final SpringApp.DefinitionStages.WithCreate creator = this.getParent().getRemote().apps().define(appName).withDefaultActiveDeployment();
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating app({0})...", appName));
        final SpringApp remote = creator.create();
        messager.success(AzureString.format("App({0}) is successfully created.", appName));
        return remote;
    }

    @Override
    protected SpringApp updateResourceInAzure(SpringApp remote, Object cfg) {
        final SpringCloudAppConfig config = (SpringCloudAppConfig) cfg;
        final SpringApp.Update update = remote.update();

        final String oldDeploymentName = remote.activeDeploymentName();
        final Boolean oldPublic = remote.isPublic();
        final PersistentDisk oldDisk = remote.persistentDisk();

        final String newDeploymentName = config.getActiveDeploymentName();
        final Boolean newPublic = config.getIsPublic();
        final Boolean newEnableDisk = config.getDeployment().getEnablePersistentStorage();

        boolean skippable = true;
        if (StringUtils.isNotBlank(newDeploymentName) && !Objects.equals(oldDeploymentName, newDeploymentName)) {
            skippable = false;
            update.withActiveDeployment(newDeploymentName);
        }

        if (Objects.nonNull(newPublic) && !Objects.equals(oldPublic, newPublic)) {
            skippable = false;
            if (newPublic) {
                update.withDefaultPublicEndpoint();
            } else {
                update.withoutDefaultPublicEndpoint();
            }
        }

        final boolean enabled = Objects.nonNull(oldDisk) && oldDisk.sizeInGB() > 0;
        if (Objects.nonNull(newEnableDisk) && !Objects.equals(newEnableDisk, enabled)) {
            skippable = false;
            if (newEnableDisk) {
                if (remote.parent().sku().tier().toLowerCase().startsWith("s")) {
                    update.withPersistentDisk(STANDARD_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH);
                } else {
                    update.withPersistentDisk(BASIC_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH);
                }
            } else {
                update.withPersistentDisk(0, null);
            }
        }

        final IAzureMessager messager = AzureMessager.getMessager();
        if (!skippable) {
            messager.info(AzureString.format("Start updating app({0})...", remote.name()));
            final SpringApp r = update.apply();
            messager.success(AzureString.format("App({0}) is successfully updated.", remote.name()));
            messager.warning(UPDATE_APP_WARNING);
            return r;
        }
        return remote;
    }

    @Override
    protected SpringCloudApp createNewResource(String name, String resourceGroup, Object config) {
        return new SpringCloudApp(name, this);
    }

    @Nonnull
    protected SpringCloudApp wrap(SpringApp remote) {
        return new SpringCloudApp(remote, this);
    }
}
