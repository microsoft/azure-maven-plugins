/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.implementation.SpringAppDeploymentImpl;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.appplatform.models.UserSourceType;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import lombok.Data;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SpringCloudDeploymentDraft extends SpringCloudDeployment
    implements AzResource.Draft<SpringCloudDeployment, SpringAppDeployment>, InvocationHandler {

    private static final String DEFAULT_RUNTIME_VERSION = SpringCloudJavaVersion.JAVA_8;
    private static final String RUNTIME_VERSION_PATTERN = "[Jj]ava((\\s)?|_)(8|11)$";

    @Delegate
    private final IConfig configProxy;
    private Config config;

    protected SpringCloudDeploymentDraft(@Nonnull String name, @Nonnull SpringCloudDeploymentModule module) {
        super(name, module);
        this.setStatus(Status.DRAFT);
        this.configProxy = (IConfig) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{IConfig.class}, this);
    }

    public void setConfig(SpringCloudDeploymentConfig deploymentConfig) {
        this.setCpu(deploymentConfig.getCpu());
        this.setMemoryInGB(deploymentConfig.getMemoryInGB());
        this.setInstanceNum(deploymentConfig.getInstanceCount());
        this.setEnvironmentVariables(deploymentConfig.getEnvironment());
        this.setRuntimeVersion(deploymentConfig.getRuntimeVersion());
        this.setJvmOptions(deploymentConfig.getJvmOptions());
        this.setArtifact(deploymentConfig.getArtifact());
    }

    public SpringCloudDeploymentConfig getConfig() {
        return SpringCloudDeploymentConfig.builder()
            .deploymentName(this.getName())
            .cpu(this.getCpu())
            .memoryInGB(this.getMemoryInGB())
            .instanceCount(this.getInstanceNum())
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

    @AzureOperation(
        name = "springcloud.create_deployment.deployment|app",
        params = {"this.getName()", "this.getParent().getName()"},
        type = AzureOperation.Type.SERVICE
    )
    public SpringAppDeployment createResourceInAzure() {
        final String name = this.getName();
        final SpringApp app = Objects.requireNonNull(this.getParent().getRemote());
        final SpringAppDeploymentImpl create = ((SpringAppDeploymentImpl) app.deployments().define(name));
        create.withExistingSource(UserSourceType.JAR, "<default>");
        modify(create);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating deployment({0})...", name));
        SpringAppDeployment deployment = create.create();
        messager.success(AzureString.format("Deployment({0}) is successfully created", name));
        deployment = this.scaleDeploymentInAzure(deployment);
        return deployment;
    }

    @Override
    @AzureOperation(
        name = "springcloud.update_deployment.deployment|app",
        params = {"this.getName()", "this.getParent().getName()"},
        type = AzureOperation.Type.SERVICE
    )
    public SpringAppDeployment updateResourceInAzure(@Nonnull SpringAppDeployment deployment) {
        final SpringAppDeploymentImpl update = ((SpringAppDeploymentImpl) Objects.requireNonNull(deployment).update());
        if (modify(update)) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating deployment({0})...", deployment.name()));
            deployment = this.doModify(() -> update.apply(), Status.UPDATING);
            messager.success(AzureString.format("Deployment({0}) is successfully updated", deployment.name()));
        }
        deployment = this.scaleDeploymentInAzure(deployment);
        return deployment;
    }

    @AzureOperation(
        name = "springcloud.scale_deployment.deployment|app",
        params = {"this.getName()", "this.getParent().getName()"},
        type = AzureOperation.Type.SERVICE
    )
    SpringAppDeployment scaleDeploymentInAzure(SpringAppDeployment deployment) {
        final SpringAppDeployment.Update update = deployment.update();
        boolean modified = scale(deployment, update);
        if (modified) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start scaling deployment({0})...", deployment.name()));
            deployment = this.doModify(() -> update.apply(), Status.SCALING);
            messager.success(AzureString.format("Deployment({0}) is successfully scaled.", deployment.name()));
        }
        return deployment;
    }

    boolean modify(@Nonnull SpringAppDeploymentImpl deployment) {
        final Map<String, String> newEnv = this.getEnvironmentVariables();
        final String newJvmOptions = this.getJvmOptions();
        final String newVersion = this.getRuntimeVersion();
        final File newArtifact = Optional.ofNullable(config.artifact).map(IArtifact::getFile).orElse(null);

        final Map<String, String> oldEnv = super.getEnvironmentVariables();
        final boolean modified = (!Objects.equals(newEnv, oldEnv) && Objects.nonNull(newEnv)) ||
            (!Objects.equals(newJvmOptions, super.getJvmOptions()) && Objects.nonNull(newJvmOptions)) ||
            (!Objects.equals(newVersion, super.getRuntimeVersion()) && Objects.nonNull(newVersion)) ||
            (Objects.nonNull(newArtifact));
        if (modified) {
            if (Objects.nonNull(newEnv)) {
                Optional.ofNullable(oldEnv).ifPresent((e) -> e.forEach((key, value) -> deployment.withoutEnvironment(key)));
                Optional.of(newEnv).ifPresent((e) -> e.forEach(deployment::withEnvironment));
            }
            Optional.ofNullable(newJvmOptions).ifPresent(deployment::withJvmOptions);
            Optional.ofNullable(newVersion).ifPresent(v -> deployment.withRuntime(RuntimeVersion.fromString(formalizeRuntimeVersion(v))));
            Optional.ofNullable(newArtifact).ifPresent(deployment::withJarFile);
        }
        return modified;
    }

    private boolean scale(SpringAppDeployment deployment, SpringAppDeployment.Update update) {
        final Integer newCpu = this.getCpu();
        final Integer newMemoryInGB = this.getMemoryInGB();
        final Integer newInstanceNum = this.getInstanceNum();
        final boolean scaled = (!Objects.equals(super.getCpu(), newCpu) && Objects.nonNull(newCpu)) ||
            (!Objects.equals(super.getMemoryInGB(), newMemoryInGB) && Objects.nonNull(newMemoryInGB)) ||
            (!Objects.equals(deployment.instances().size(), newInstanceNum) && Objects.nonNull(newInstanceNum));
        if (scaled) {
            Optional.ofNullable(newCpu).ifPresent(update::withCpu);
            Optional.ofNullable(newMemoryInGB).ifPresent(update::withMemory);
            Optional.ofNullable(newInstanceNum).ifPresent(update::withInstance);
        }
        return scaled;
    }

    private static String formalizeRuntimeVersion(String runtimeVersion) {
        if (StringUtils.isEmpty(runtimeVersion)) {
            return DEFAULT_RUNTIME_VERSION;
        }
        final String fixedRuntimeVersion = StringUtils.trim(runtimeVersion);
        final Matcher matcher = Pattern.compile(RUNTIME_VERSION_PATTERN).matcher(fixedRuntimeVersion);
        if (matcher.matches()) {
            return Objects.equals(matcher.group(3), "8") ? SpringCloudJavaVersion.JAVA_8 : SpringCloudJavaVersion.JAVA_11;
        } else {
            log.warn("{} is not a valid runtime version, supported values are Java 8 and Java 11, using Java 8 in this deployment.", fixedRuntimeVersion);
            return DEFAULT_RUNTIME_VERSION;
        }
    }

    @Override
    public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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

    private Object invokeSuper(Method method, Object[] args) throws Throwable {
        final Class<?>[] classes = Arrays.stream(args).map(Object::getClass).toArray(value -> new Class<?>[0]);
        final MethodType type = MethodType.methodType(method.getReturnType(), classes);
        final var handle = MethodHandles.lookup().findSpecial(SpringCloudDeployment.class, method.getName(), type, this.getClass()).bindTo(this);
        return handle.invokeWithArguments(args);
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
        Integer cpu;
        @Nullable
        Integer memoryInGB;
        @Nullable
        Integer instanceNum;
    }


    private interface IConfig {
        void setEnvironmentVariables(Map<String, String> environmentVariables);

        void setJvmOptions(String jvmOptions);

        void setRuntimeVersion(String runtimeVersion);

        void setArtifact(IArtifact artifact);

        void setCpu(Integer cpu);

        void setMemoryInGB(Integer memoryInGB);

        void setInstanceNum(Integer instanceNum);

        Map<String, String> getEnvironmentVariables();

        String getJvmOptions();

        String getRuntimeVersion();

        IArtifact getArtifact();

        Integer getCpu();

        Integer getMemoryInGB();

        Integer getInstanceNum();
    }
}