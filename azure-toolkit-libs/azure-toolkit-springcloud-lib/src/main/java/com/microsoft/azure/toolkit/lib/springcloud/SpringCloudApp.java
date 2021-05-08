/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.PersistentDisk;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.springcloud.model.AzureRemotableArtifact;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudAppManager;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudDeploymentManager;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpringCloudApp implements IAzureEntityManager<SpringCloudAppEntity> {
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";

    @Getter
    private final SpringCloudCluster cluster;
    private final SpringCloudAppManager appManager;
    private final SpringCloudAppEntity local;
    private final SpringCloudDeploymentManager deploymentManager;
    private SpringCloudAppEntity remote;
    private boolean refreshed;

    public SpringCloudApp(SpringCloudAppEntity app, SpringCloudCluster cluster) {
        this.local = app;
        this.cluster = cluster;
        this.appManager = new SpringCloudAppManager(cluster.getClient());
        this.deploymentManager = new SpringCloudDeploymentManager(cluster.getClient());
    }

    @Override
    public boolean exists() {
        if (!this.refreshed) {
            this.refresh();
        }
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

    public List<SpringCloudDeployment> deployments() {
        final SpringCloudAppEntity app = this.entity();
        return this.appManager.getDeployments(app).stream().map(this::deployment).collect(Collectors.toList());
    }

    public SpringCloudCluster cluster() {
        return this.cluster;
    }

    public SpringCloudApp start() {
        this.deploymentManager.start(this.getActiveDeploymentName(), this.entity());
        return this;
    }

    public SpringCloudApp stop() {
        this.deploymentManager.stop(this.getActiveDeploymentName(), this.entity());
        return this;
    }

    public SpringCloudApp restart() {
        this.deploymentManager.restart(this.getActiveDeploymentName(), this.entity());
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
        this.refreshed = true;
        return this;
    }

    public Uploader uploadArtifact(String path) {
        return new Uploader(path, this);
    }

    public Creator create() {
        return new Creator(this);
    }

    @Nonnull
    public Updater update() {
        return new Updater(this);
    }

    public String getActiveDeploymentName() {
        if (!this.refreshed) {
            this.refresh();
        }
        return Optional.ofNullable(this.remote).map(r -> this.remote.getActiveDeployment()).orElse(null);
    }

    public static class Uploader implements ICommittable<SpringCloudApp> {

        private final SpringCloudApp app;
        private final String path;
        @Getter
        private final AzureRemotableArtifact artifact;

        public Uploader(String path, SpringCloudApp app) {
            this.path = path;
            this.app = app;
            this.artifact = new AzureRemotableArtifact(this.path);
        }

        @SneakyThrows
        @Override
        public SpringCloudApp commit() {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(String.format("Start uploading artifact(%s) to App(%s)...", messager.value(this.path), messager.value(this.app.name())));
            final ResourceUploadDefinitionInner definition = this.app.appManager.uploadArtifact(this.path, this.app.entity());
            messager.success(String.format("Artifact(%s) is successfully uploaded to App(%s)...", messager.value(this.path), messager.value(this.app.name())));
            this.artifact.setRemotePath(definition.relativePath());
            this.artifact.setUploadUrl(definition.uploadUrl());
            return this.app;
        }
    }

    public static class Updater implements ICommittable<SpringCloudApp> {
        protected final SpringCloudApp app;
        protected final AppResourceInner resource;

        public Updater(SpringCloudApp app) {
            this.app = app;
            this.resource = new AppResourceInner();
        }

        public Updater activate(String deploymentName) {
            final String oldDeploymentName = Optional.ofNullable(this.app.remote).map(SpringCloudAppEntity::getActiveDeployment).orElse(null);
            if (StringUtils.isNotBlank(deploymentName) && !Objects.equals(oldDeploymentName, deploymentName)) {
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

        @Override
        public SpringCloudApp commit() {
            final IAzureMessager messager = AzureMessager.getMessager();
            if (this.isSkippable()) {
                messager.info(String.format("skip updating app(%s) since its properties is not changed.", this.app.name()));
            } else {
                messager.info(String.format("Start updating app(%s)...", messager.value(this.app.name())));
                this.app.remote = this.app.appManager.update(this.resource, this.app.entity());
                messager.success(String.format("App(%s) is successfully updated.", messager.value(this.app.name())));
                messager.warning(UPDATE_APP_WARNING);
            }
            return this.app;
        }

        public boolean isSkippable() {
            return Objects.isNull(this.resource.properties()) && Objects.isNull(this.resource.location());
        }
    }

    public static class Creator extends Updater {
        public Creator(SpringCloudApp app) {
            super(app);
        }

        public SpringCloudApp commit() {
            final String appName = this.app.name();
            final SpringCloudClusterEntity cluster = this.app.cluster.entity();
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(String.format("Start creating app(%s)...", messager.value(appName)));
            this.app.remote = this.app.appManager.create(this.resource, appName, cluster);
            messager.success(String.format("App(%s) is successfully created.", messager.value(appName)));
            return this.app;
        }
    }
}
