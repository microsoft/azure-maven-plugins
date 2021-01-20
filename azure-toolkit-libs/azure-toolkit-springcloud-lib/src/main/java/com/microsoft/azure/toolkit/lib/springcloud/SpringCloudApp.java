/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appplatform.v2020_07_01.PersistentDisk;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.toolkit.lib.common.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudClusterEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudDeploymentEntity;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudAppManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class SpringCloudApp implements IAzureEntityManager<SpringCloudAppEntity> {
    @Getter
    private final SpringCloudCluster cluster;
    private final SpringCloudAppManager appManager;
    private final SpringCloudAppEntity local;
    private SpringCloudAppEntity remote;

    public SpringCloudApp(SpringCloudAppEntity app, SpringCloudCluster cluster) {
        this.local = app;
        this.cluster = cluster;
        this.appManager = new SpringCloudAppManager(cluster.getClient());
    }

    @Override
    public boolean exists() {
        this.refresh();
        return Objects.nonNull(this.remote);
    }

    public SpringCloudAppEntity entity() {
        return Objects.nonNull(this.remote) ? this.remote : this.local;
    }

    public SpringCloudDeployment deployment(SpringCloudDeploymentEntity deployment) {
        return new SpringCloudDeployment(deployment, this);
    }

    public SpringCloudDeployment deployment(final String name) {
        final SpringCloudAppEntity app = this.entity();
        return new SpringCloudDeployment(SpringCloudDeploymentEntity.fromName(name, app), this);
    }

    public SpringCloudCluster cluster() {
        return this.cluster;
    }

    public SpringCloudApp start() {
        this.appManager.start(this.entity());
        return this;
    }

    public SpringCloudApp stop() {
        this.appManager.stop(this.entity());
        return this;
    }

    public SpringCloudApp restart() {
        this.appManager.restart(this.entity());
        return this;
    }

    public SpringCloudApp remove() {
        this.appManager.remove(this.entity());
        return this;
    }

    @Nonnull
    @Override
    public SpringCloudApp refresh() {
        final SpringCloudAppEntity app = this.entity();
        final SpringCloudClusterEntity cluster = this.cluster.entity();
        this.remote = this.appManager.get(app.getName(), cluster);
        return this;
    }

    public String uploadArtifact(String path) throws AzureExecutionException {
        return this.appManager.uploadArtifact(path, this.entity()).relativePath();
    }

    public ResourceUploadDefinitionInner getUploadDefinition() {
        return this.appManager.getUploadDefinition(this.entity());
    }

    public Creator create() {
        return new Creator(this);
    }

    @Nonnull
    public Updater update() {
        return new Updater(this);
    }

    public static class Updater {
        protected final SpringCloudApp app;
        protected final AppResourceInner resource;

        public Updater(SpringCloudApp app) {
            this.app = app;
            this.resource = new AppResourceInner();
        }

        public Updater activate(String deploymentName) {
            final String oldDeploymentName = Optional.ofNullable(this.app.remote).map(e -> e.getInner().properties().activeDeploymentName()).orElse(null);
            if (StringUtils.isNoneEmpty(deploymentName) && !Objects.equals(oldDeploymentName, deploymentName)) {
                AzureSpringCloudConfigUtils.getOrCreateProperties(this.resource, this.app)
                    .withActiveDeploymentName(deploymentName);
            }
            return this;
        }

        public Updater setPublic(Boolean isPublic) {
            final Boolean oldPublic = Optional.ofNullable(this.app.remote).map(e -> e.getInner().properties().publicProperty()).orElse(null);
            if (Objects.nonNull(isPublic) && !Objects.equals(oldPublic, isPublic)) {
                AzureSpringCloudConfigUtils.getOrCreateProperties(this.resource, this.app)
                    .withPublicProperty(isPublic);
            }
            return this;
        }

        public Updater enablePersistentDisk(Boolean enable) {
            final boolean enabled = Optional.ofNullable(this.app.remote).map(e -> e.getInner().properties().persistentDisk())
                .filter(d -> d.sizeInGB() > 0).isPresent();
            if (Objects.nonNull(enable) && !Objects.equals(enable, enabled)) {
                final PersistentDisk newDisk = enable ? AzureSpringCloudConfigUtils.getOrCreatePersistentDisk(this.resource, this.app) : null;
                AzureSpringCloudConfigUtils.getOrCreateProperties(this.resource, this.app)
                    .withPersistentDisk(newDisk);
            }
            return this;
        }

        public SpringCloudApp commit() {
            if (Objects.isNull(this.resource.properties()) && Objects.isNull(this.resource.location())) {
                log.info("skip updating app({}) since its properties is not changed.", this.app.entity().getName());
            } else {
                this.app.remote = this.app.appManager.update(this.resource, this.app.entity());
            }
            return this.app;
        }
    }

    public static class Creator extends Updater {
        public Creator(SpringCloudApp app) {
            super(app);
        }

        public SpringCloudApp commit() {
            final String appName = this.app.entity().getName();
            final SpringCloudClusterEntity cluster = this.app.cluster.entity();
            this.app.remote = this.app.appManager.create(this.resource, appName, cluster);
            return this.app;
        }
    }
}
