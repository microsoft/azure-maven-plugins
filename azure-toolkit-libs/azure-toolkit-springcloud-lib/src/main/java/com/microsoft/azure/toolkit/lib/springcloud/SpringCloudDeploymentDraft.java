/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.fluent.models.DeploymentResourceInner;
import com.azure.resourcemanager.appplatform.implementation.SpringAppDeploymentImpl;
import com.azure.resourcemanager.appplatform.models.DeploymentResourceProperties;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.Scale;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.appplatform.models.UserSourceType;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SpringCloudDeploymentDraft extends SpringCloudDeployment
    implements AzResource.Draft<SpringCloudDeployment, SpringAppDeployment>, InvocationHandler {

    public static final RuntimeVersion DEFAULT_RUNTIME_VERSION = RuntimeVersion.JAVA_11;
    private static final String RUNTIME_VERSION_PATTERN = "[Jj]ava((\\s)?|_)(8|11|17)$";

    @Nonnull
    @Delegate
    private final IConfig configProxy;
    @Getter
    @Nullable
    private final SpringCloudDeployment origin;
    @Nullable
    private Config config;

    protected SpringCloudDeploymentDraft(@Nonnull String name, @Nonnull SpringCloudDeploymentModule module) {
        super(name, module);
        this.origin = null;
        this.configProxy = (IConfig) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{IConfig.class}, this);
    }

    protected SpringCloudDeploymentDraft(@Nonnull SpringCloudDeployment origin) {
        super(origin);
        this.origin = origin;
        this.configProxy = (IConfig) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{IConfig.class}, this);
    }

    public void setConfig(@Nonnull SpringCloudDeploymentConfig deploymentConfig) {
        this.setCpu(deploymentConfig.getCpu());
        this.setMemoryInGB(deploymentConfig.getMemoryInGB());
        this.setCapacity(deploymentConfig.getCapacity());
        this.setEnvironmentVariables(deploymentConfig.getEnvironment());
        this.setRuntimeVersion(deploymentConfig.getRuntimeVersion());
        this.setJvmOptions(deploymentConfig.getJvmOptions());
        this.setArtifact(deploymentConfig.getArtifact());
    }

    @Nonnull
    public SpringCloudDeploymentConfig getConfig() {
        return SpringCloudDeploymentConfig.builder()
            .deploymentName(this.getName())
            .cpu(this.getCpu())
            .memoryInGB(this.getMemoryInGB())
            .capacity(this.getCapacity())
            .jvmOptions(this.getJvmOptions())
            .runtimeVersion(this.getRuntimeVersion())
            .environment(this.getEnvironmentVariables())
            .artifact(this.getArtifact())
            .build();
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/springcloud.create_app_deployment.deployment", params = {"this.getName()"})
    public SpringAppDeployment createResourceInAzure() {
        final String name = this.getName();
        final SpringApp app = Objects.requireNonNull(this.getParent().getRemote());
        final SpringAppDeploymentImpl create = ((SpringAppDeploymentImpl) app.deployments().define(name));
        create.withExistingSource(UserSourceType.JAR, "<default>");
        create.withActivation();
        modify(create);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating deployment({0})...", name));
        SpringAppDeployment deployment = this.doModify(() -> create.create(), Status.CREATING);
        messager.success(AzureString.format("Deployment({0}) is successfully created", name));
        deployment = this.scaleDeploymentInAzure(deployment);
        return deployment;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/springcloud.update_app_deployment.deployment", params = {"this.getName()"})
    public SpringAppDeployment updateResourceInAzure(@Nonnull SpringAppDeployment deployment) {
        final SpringAppDeploymentImpl update = ((SpringAppDeploymentImpl) Objects.requireNonNull(deployment).update());
        if (modify(update)) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating deployment({0})...", deployment.name()));
            deployment = update.apply();
            messager.success(AzureString.format("Deployment({0}) is successfully updated.", deployment.name()));
        }
        deployment = this.scaleDeploymentInAzure(deployment);
        return deployment;
    }

    @Nonnull
    @AzureOperation(name = "azure/springcloud.scale_app_instances.deployment", params = {"this.getName()"})
    SpringAppDeployment scaleDeploymentInAzure(@Nonnull SpringAppDeployment deployment) {
        final SpringAppDeployment.Update update = deployment.update();
        boolean modified = scale(deployment, update);
        if (modified) {
            final IAzureMessager messager = AzureMessager.getMessager();
            this.doModify(() -> {
                messager.info(AzureString.format("Start scaling deployment({0})...", deployment.name()));
                update.apply();
                this.getSubModules().forEach(AbstractAzResourceModule::refresh);
                messager.success(AzureString.format("Deployment({0}) is successfully scaled.", deployment.name()));
            }, Status.SCALING);
        }
        return deployment;
    }

    boolean modify(@Nonnull SpringAppDeploymentImpl deployment) {
        final Map<String, String> newEnv = Utils.emptyToNull(this.getEnvironmentVariables());
        final String newJvmOptions = Utils.emptyToNull(this.getJvmOptions());
        final String newVersion = Utils.emptyToNull(this.getRuntimeVersion());
        final File newArtifact = Optional.ofNullable(config).map(c -> c.artifact).map(IArtifact::getFile).orElse(null);

        final Map<String, String> oldEnv = Utils.emptyToNull(super.getEnvironmentVariables());
        final String oldJvmOptions = Utils.emptyToNull(super.getJvmOptions());
        final String oldVersion = Utils.emptyToNull(super.getRuntimeVersion());
        final boolean modified = (!Objects.equals(newEnv, oldEnv) && Objects.nonNull(newEnv)) ||
            (!Objects.equals(newJvmOptions, oldJvmOptions) && Objects.nonNull(newJvmOptions)) ||
            (!Objects.equals(newVersion, oldVersion) && Objects.nonNull(newVersion)) ||
            (Objects.nonNull(newArtifact));
        if (modified) {
            if (Objects.nonNull(newEnv)) {
                Optional.ofNullable(oldEnv).ifPresent(e -> new HashSet<>(e.keySet()).forEach(deployment::withoutEnvironment));
                Optional.of(newEnv).ifPresent((e) -> e.forEach(deployment::withEnvironment));
            }
            Optional.ofNullable(newJvmOptions).ifPresent(deployment::withJvmOptions);
            Optional.ofNullable(newVersion).ifPresent(v -> deployment.withRuntime(formalizeRuntimeVersion(v)));
            Optional.ofNullable(newArtifact).ifPresent(deployment::withJarFile);
        }
        return modified;
    }

    private boolean scale(@Nonnull SpringAppDeployment deployment, @Nonnull SpringAppDeployment.Update update) {
        final Double newCpu = this.getCpu();
        final Double newMemoryInGB = this.getMemoryInGB();
        final Integer capacity = this.getCapacity();
        boolean scaled = (!Objects.equals(deployment.cpu(), newCpu) && Objects.nonNull(newCpu)) ||
            (!Objects.equals(deployment.memoryInGB(), newMemoryInGB) && Objects.nonNull(newMemoryInGB));
        if (scaled) {
            Optional.ofNullable(newCpu).ifPresent(update::withCpu);
            Optional.ofNullable(newMemoryInGB).ifPresent(update::withMemory);
        }

        if(Objects.nonNull(capacity)) {
            if (this.getParent().getParent().isConsumptionTier()) {
                final Integer max = Optional.ofNullable(deployment.innerModel())
                    .map(DeploymentResourceInner::properties)
                    .map(DeploymentResourceProperties::deploymentSettings)
                    .map(DeploymentSettings::scale)
                    .map(Scale::maxReplicas)
                    .orElse(0);
                if (!Objects.equals(capacity, max)) {
                    scaled = true;
                    deployment.innerModel().properties().deploymentSettings().withScale(new Scale().withMaxReplicas(capacity));
                }
            } else {
                if (!Objects.equals(deployment.parent().parent().sku().capacity(), capacity)) {
                    scaled = true;
                    Optional.of(capacity).ifPresent(update::withInstance);
                }
            }
        }
        return scaled;
    }

    @Nonnull
    public static RuntimeVersion formalizeRuntimeVersion(String runtimeVersion) {
        if (StringUtils.isEmpty(runtimeVersion)) {
            return DEFAULT_RUNTIME_VERSION;
        }
        final String fixedRuntimeVersion = StringUtils.trim(runtimeVersion);
        final Matcher matcher = Pattern.compile(RUNTIME_VERSION_PATTERN).matcher(fixedRuntimeVersion);
        if (matcher.matches()) {
            final String v = matcher.group(3);
            return Objects.equals(v, "17") ? RuntimeVersion.JAVA_17 :
                Objects.equals(v, "11") ? RuntimeVersion.JAVA_11 : RuntimeVersion.JAVA_8;
        } else {
            log.warn("{} is not a valid runtime version, supported values are 'Java 8', 'Java 11' and 'Java 17', using Java 8 in this deployment.", fixedRuntimeVersion);
            return DEFAULT_RUNTIME_VERSION;
        }
    }

    @Nullable
    @Override
    public synchronized Object invoke(Object proxy, @Nonnull Method method, Object[] args) throws Throwable {
        args = ObjectUtils.firstNonNull(args, new Object[0]);
        if (method.getName().startsWith("set")) {
            synchronized (this) {
                this.config = ObjectUtils.firstNonNull(this.config, new Config());
                return method.invoke(config, args);
            }
        } else {
            final Set<String> excludes = Collections.singleton("getArtifact");
            final Object result = Objects.nonNull(config) ? method.invoke(config, args) : null;
            return Objects.nonNull(result) || excludes.contains(method.getName()) ? result : invokeSuper(method, args);
        }
    }

    private Object invokeSuper(@Nonnull Method method, @Nonnull Object[] args) throws Throwable {
        final Class<?>[] classes = Arrays.stream(args).map(Object::getClass).toArray(value -> new Class<?>[0]);
        final MethodType type = MethodType.methodType(method.getReturnType(), classes);
        final MethodHandle handle = MethodHandles.lookup().findSpecial(SpringCloudDeployment.class, method.getName(), type, this.getClass()).bindTo(this);
        return handle.invokeWithArguments(args);
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) || Objects.isNull(this.config.getArtifact()) ||
            Objects.isNull(this.config.getEnvironmentVariables()) || Objects.equals(this.config.getEnvironmentVariables(), super.getEnvironmentVariables()) ||
            Objects.isNull(this.config.getJvmOptions()) || Objects.equals(this.config.getJvmOptions(), super.getJvmOptions()) ||
            Objects.isNull(this.config.getRuntimeVersion()) || Objects.equals(this.config.getRuntimeVersion(), super.getRuntimeVersion()) ||
            Objects.isNull(this.config.getCpu()) || Objects.equals(this.config.getCpu(), super.getCpu()) ||
            Objects.isNull(this.config.getMemoryInGB()) || Objects.equals(this.config.getMemoryInGB(), super.getMemoryInGB()) ||
            Objects.isNull(this.config.getCapacity()) || Objects.equals(this.config.getCapacity(), super.getCapacity());
        return !notModified;
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    private static class Config implements IConfig {
        @Nullable
        Map<String, String> environmentVariables;
        @Nullable
        String jvmOptions;
        @Nullable
        String runtimeVersion;
        @Nullable
        IArtifact artifact;
        @Nullable
        Double cpu;
        @Nullable
        Double memoryInGB;
        @Nullable
        Integer capacity;
    }


    private interface IConfig {
        void setEnvironmentVariables(Map<String, String> environmentVariables);

        void setJvmOptions(String jvmOptions);

        void setRuntimeVersion(String runtimeVersion);

        void setArtifact(IArtifact artifact);

        void setCpu(Double cpu);

        void setMemoryInGB(Double memoryInGB);

        void setCapacity(Integer capacity);

        @Nullable
        Map<String, String> getEnvironmentVariables();

        @Nullable
        String getJvmOptions();

        @Nullable
        String getRuntimeVersion();

        @Nullable
        IArtifact getArtifact();

        @Nullable
        Double getCpu();

        @Nullable
        Double getMemoryInGB();

        @Nullable
        Integer getCapacity();
    }
}