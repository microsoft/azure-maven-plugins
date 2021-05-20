/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appplatform.implementation.SpringAppImpl;
import com.azure.resourcemanager.appplatform.models.PersistentDisk;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation.Type;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SpringCloudApp implements IAzureEntityManager<SpringCloudAppEntity>, AzureOperationEvent.Source<SpringCloudApp> {
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";

    @Getter
    @Nonnull
    final SpringCloudCluster cluster;
    @Nonnull
    private final SpringCloudAppEntity local;
    @Nullable
    @Getter(AccessLevel.PACKAGE)
    private SpringApp remote;
    private boolean refreshed;

    public SpringCloudApp(@Nonnull SpringCloudAppEntity app, @Nonnull SpringCloudCluster cluster) {
        this.local = app;
        this.remote = this.local.getRemote();
        this.cluster = cluster;
    }

    @Override
    public boolean exists() {
        if (Objects.isNull(this.remote) && !this.refreshed) {
            this.refresh();
        }
        return Objects.nonNull(this.remote);
    }

    @Nonnull
    public SpringCloudAppEntity entity() {
        final SpringApp remote = Objects.nonNull(this.remote) ? this.remote : this.local.getRemote();
        if (Objects.isNull(remote)) {
            return this.local;
        }
        // prevent inconsistent properties between local and remote when local's properties is modified.
        return new SpringCloudAppEntity(remote, this.cluster.entity());
    }

    @Nonnull
    public SpringCloudDeployment deployment(final String name) {
        if (this.exists()) {
            try {
                final SpringAppDeployment deployment = Objects.requireNonNull(this.remote).deployments().getByName(name);
                return this.deployment(deployment);
            } catch (ManagementException ignored) {
            }
        }
        return this.deployment(new SpringCloudDeploymentEntity(name, this.entity()));
    }

    @Nonnull
    SpringCloudDeployment deployment(final SpringAppDeployment deployment) {
        return this.deployment(new SpringCloudDeploymentEntity(deployment, this.entity()));
    }

    @Nonnull
    public SpringCloudDeployment deployment(SpringCloudDeploymentEntity deployment) {
        return new SpringCloudDeployment(deployment, this);
    }

    @Nullable
    public SpringCloudDeployment activeDeployment() {
        assert this.exists() && Objects.nonNull(this.remote) : String.format("app(%s) not exist", this.name());
        final SpringAppDeployment active = this.remote.getActiveDeployment();
        if (Objects.isNull(active)) {
            return null;
        }
        return this.deployment(active);
    }

    @Nonnull
    public List<SpringCloudDeployment> deployments() {
        if (this.exists()) {
            return Objects.requireNonNull(this.remote).deployments().list().stream().map(this::deployment).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @AzureOperation(name = "springcloud|app.start", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudApp start() {
        this.deployment(this.getActiveDeploymentName()).start();
        return this;
    }

    @AzureOperation(name = "springcloud|app.stop", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudApp stop() {
        this.deployment(this.getActiveDeploymentName()).stop();
        return this;
    }

    @AzureOperation(name = "springcloud|app.restart", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudApp restart() {
        this.deployment(this.getActiveDeploymentName()).restart();
        return this;
    }

    @AzureOperation(name = "springcloud|app.remove", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudApp remove() {
        if (this.exists() && Objects.nonNull(this.remote)) {
            this.remote.parent().apps().deleteByName(this.name());
        }
        return this;
    }

    @Nonnull
    @Override
    public SpringCloudApp refresh() {
        final SpringCloudApp _this = this.cluster.app(this.entity().getName());
        this.updateRemote(_this.entity().getRemote());
        this.refreshed = true;
        return this;
    }

    private void updateRemote(SpringApp remote) {
        this.remote = remote;
        this.local.setRemote(this.remote);
    }

    public Creator create() {
        assert this.cluster.exists() : String.format("cluster(%s) not exist", this.cluster.name());
        return new Creator(this);
    }

    @Nonnull
    public Updater update() throws AzureToolkitRuntimeException {
        assert this.exists() : String.format("app(%s) not exist", this.name());
        return new Updater(this);
    }

    @Nullable
    public String getActiveDeploymentName() {
        if (this.exists() && Objects.nonNull(this.remote)) {
            return this.remote.getActiveDeployment().name();
        }
        return null;
    }

    public static abstract class Modifier implements ICommittable<SpringCloudApp>, AzureOperationEvent.Source<SpringCloudApp> {
        public static final String DEFAULT_DISK_MOUNT_PATH = "/persistent";
        /**
         * @see <a href=https://azure.microsoft.com/en-us/pricing/details/spring-cloud/>Pricing - Azure Spring Cloud</a>
         */
        public static final int BASIC_TIER_DEFAULT_DISK_SIZE = 1;
        /**
         * @see <a href=https://azure.microsoft.com/en-us/pricing/details/spring-cloud/>Pricing - Azure Spring Cloud</a>
         */
        public static final int STANDARD_TIER_DEFAULT_DISK_SIZE = 50;

        @Nonnull
        protected final SpringCloudApp app;
        protected SpringAppImpl modifier;
        @Getter
        protected boolean skippable = true;

        protected Modifier(@Nonnull SpringCloudApp app) {
            this.app = app;
        }

        public Modifier activate(String deploymentName) {
            final String oldDeploymentName = this.app.getActiveDeploymentName();
            if (StringUtils.isNotBlank(deploymentName) && !Objects.equals(oldDeploymentName, deploymentName)) {
                this.skippable = false;
                this.modifier.withActiveDeployment(deploymentName);
            }
            return this;
        }

        public Modifier setPublic(Boolean isPublic) {
            final Boolean oldPublic = this.modifier.isPublic();
            if (Objects.nonNull(isPublic) && !Objects.equals(oldPublic, isPublic)) {
                this.skippable = false;
                if (isPublic) {
                    this.modifier.withDefaultPublicEndpoint();
                } else {
                    this.modifier.withoutDefaultPublicEndpoint();
                }
            }
            return this;
        }

        public Modifier enablePersistentDisk(Boolean enable) {
            final PersistentDisk oldDisk = this.modifier.persistentDisk();
            final boolean enabled = Objects.nonNull(oldDisk) && oldDisk.sizeInGB() > 0;
            if (Objects.nonNull(enable) && !Objects.equals(enable, enabled)) {
                this.skippable = false;
                if (enable) {
                    if (Objects.requireNonNull(this.app.cluster.getRemote()).sku().tier().toLowerCase().startsWith("s")) {
                        this.modifier.withPersistentDisk(STANDARD_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH);
                    } else {
                        this.modifier.withPersistentDisk(BASIC_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH);
                    }
                } else {
                    this.modifier.withPersistentDisk(0, null);
                }
            }
            return this;
        }

        @NotNull
        @Override
        public AzureOperationEvent.Source<SpringCloudApp> getEventSource() {
            return this.app;
        }
    }

    public static class Updater extends Modifier {

        public Updater(SpringCloudApp app) {
            super(app);
            this.modifier = ((SpringAppImpl) Objects.requireNonNull(this.app.remote).update());
        }

        @Override
        @AzureOperation(name = "springcloud|app.update", params = {"this.app.name()"}, type = Type.SERVICE)
        public SpringCloudApp commit() {
            final IAzureMessager messager = AzureMessager.getMessager();
            if (this.skippable) {
                messager.info(String.format("skip updating app(%s) since nothing is changed.", this.app.name()));
            } else {
                messager.info(String.format("Start updating app(%s)...", messager.value(this.app.name())));
                this.app.updateRemote(this.modifier.apply());
                messager.success(String.format("App(%s) is successfully updated.", messager.value(this.app.name())));
                messager.warning(UPDATE_APP_WARNING);
            }
            return this.app;
        }
    }

    public static class Creator extends Modifier {
        private final SpringAppImpl modifier;

        public Creator(SpringCloudApp app) {
            super(app);
            this.modifier = ((SpringAppImpl) Objects.requireNonNull(this.app.cluster.getRemote()).apps().define(app.name()).withDefaultActiveDeployment());
        }

        @AzureOperation(name = "springcloud|app.create", params = {"this.app.name()"}, type = Type.SERVICE)
        public SpringCloudApp commit() {
            final String appName = this.app.name();
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(String.format("Start creating app(%s)...", messager.value(appName)));
            this.app.updateRemote(this.modifier.create());
            messager.success(String.format("App(%s) is successfully created.", messager.value(appName)));
            return this.app;
        }
    }
}
