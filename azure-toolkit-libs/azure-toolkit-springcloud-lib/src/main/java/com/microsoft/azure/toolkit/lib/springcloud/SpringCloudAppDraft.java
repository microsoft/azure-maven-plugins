/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.implementation.SpringAppImpl;
import com.azure.resourcemanager.appplatform.models.PersistentDisk;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
public class SpringCloudAppDraft extends SpringCloudApp implements AzResource.Draft<SpringCloudApp, SpringApp> {
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

    @Nullable
    private SpringCloudAppConfig config;

    SpringCloudAppDraft(@Nonnull String name, @Nonnull SpringCloudAppModule module) {
        super(name, module);
        this.setStatus(Status.DRAFT);
    }

    public boolean isPublic() {
        if (Objects.nonNull(this.config)) {
            return this.config.isPublic();
        }
        return false;
    }

    @Override
    public SpringApp createResourceInAzure() {
        final String appName = this.getName();
        final SpringService service = Objects.requireNonNull(this.getParent().getRemote());
        final SpringAppImpl create = (SpringAppImpl) service.apps().define(appName).withDefaultActiveDeployment();
        modify(create, this.config);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating app({0})...", appName));
        final SpringApp app = create.create();
        messager.success(AzureString.format("App({0}) is successfully created.", appName));
        return app;
    }

    @Override
    public SpringApp updateResourceInAzure(@Nonnull SpringApp origin) {
        final SpringAppImpl update = ((SpringAppImpl) origin.update());
        if (modify(update, config)) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating app({0})...", origin.name()));
            origin = update.apply();
            messager.success(AzureString.format("App({0}) is successfully updated.", origin.name()));
            messager.warning(UPDATE_APP_WARNING);
        }
        return origin;
    }

    boolean modify(@Nonnull SpringAppImpl app, @Nullable SpringCloudAppConfig config) {
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
        final Boolean newEnableDisk = Optional.ofNullable(config.getDeployment())
            .map(SpringCloudDeploymentConfig::getEnablePersistentStorage).orElse(null);

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
}