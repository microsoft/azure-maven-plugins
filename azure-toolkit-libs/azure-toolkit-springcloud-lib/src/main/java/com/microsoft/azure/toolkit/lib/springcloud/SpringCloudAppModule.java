/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.implementation.SpringAppImpl;
import com.azure.resourcemanager.appplatform.models.PersistentDisk;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringApps;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
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
    protected SpringApp createResourceInAzure(@Nonnull String appName, @Nonnull String resourceGroup, Object cfg) {
        final SpringCloudAppConfig config = (SpringCloudAppConfig) cfg;
        final SpringAppImpl create = (SpringAppImpl) this.getParent().getRemote().apps().define(appName).withDefaultActiveDeployment();
        modify(create, config);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating app({0})...", appName));
        final SpringApp app = create.create();
        messager.success(AzureString.format("App({0}) is successfully created.", appName));
        return app;
    }

    @Override
    protected SpringApp updateResourceInAzure(@Nonnull SpringApp app, Object cfg) {
        final SpringCloudAppConfig config = (SpringCloudAppConfig) cfg;
        final SpringAppImpl update = ((SpringAppImpl) app.update());
        if (modify(update, config)) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating app({0})...", app.name()));
            app = update.apply();
            messager.success(AzureString.format("App({0}) is successfully updated.", app.name()));
            messager.warning(UPDATE_APP_WARNING);
        }
        return app;
    }

    protected boolean modify(@Nonnull SpringAppImpl app, SpringCloudAppConfig config) {
        if (Objects.isNull(config)) {
            return false;
        }
        boolean skippable = true;
        final String oldDeploymentName = app.activeDeploymentName();
        final Boolean oldPublic = app.isPublic();
        final PersistentDisk oldDisk = app.persistentDisk();
        final boolean oldEnableDisk = Objects.nonNull(oldDisk) && oldDisk.sizeInGB() > 0;

        final String newDeploymentName = config.getActiveDeploymentName();
        final Boolean newPublic = config.getIsPublic();
        final Boolean newEnableDisk = config.getDeployment().getEnablePersistentStorage();

        if (StringUtils.isNotBlank(newDeploymentName) && !Objects.equals(oldDeploymentName, newDeploymentName)) {
            skippable = false;
            app = app.withActiveDeployment(newDeploymentName);
        }
        if (Objects.nonNull(newPublic) && !Objects.equals(oldPublic, newPublic)) {
            skippable = false;
            app = newPublic ? app.withDefaultPublicEndpoint() : app.withoutDefaultPublicEndpoint();
        }
        if (Objects.nonNull(newEnableDisk) && !Objects.equals(newEnableDisk, oldEnableDisk)) {
            skippable = false;
            app = newEnableDisk ? (app.parent().sku().tier().toLowerCase().startsWith("s") ?
                app.withPersistentDisk(STANDARD_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH) :
                app.withPersistentDisk(BASIC_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH)) :
                app.withPersistentDisk(0, null);
        }
        return !skippable;
    }

    @Override
    protected SpringCloudApp newResource(@Nonnull String name, @Nonnull String resourceGroup) {
        return new SpringCloudApp(name, this);
    }

    @Nonnull
    protected SpringCloudApp newResource(SpringApp remote) {
        return new SpringCloudApp(remote, this);
    }
}
