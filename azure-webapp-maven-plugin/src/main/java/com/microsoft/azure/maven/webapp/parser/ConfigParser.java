/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfig;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.ExpandableParameter;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class ConfigParser {

    protected AbstractWebAppMojo mojo;

    public ConfigParser(AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    public String getAppName() {
        return mojo.getAppName();
    }

    public String getResourceGroup() {
        return mojo.getResourceGroup();
    }

    public String getDeploymentSlotName() {
        return mojo.getDeploymentSlotSetting() == null ? null : mojo.getDeploymentSlotSetting().getName();
    }

    public String getDeploymentSlotConfigurationSource() {
        return mojo.getDeploymentSlotSetting() == null ? null : mojo.getDeploymentSlotSetting().getConfigurationSource();
    }

    public PricingTier getPricingTier() {
        return parseExpandableParameter(input -> {
            if (StringUtils.contains(mojo.getPricingTier(), "_")) {
                final String[] pricingParams = mojo.getPricingTier().split("_");
                return PricingTier.fromString(pricingParams[0], pricingParams[1]);
            } else {
                return PricingTier.fromString(mojo.getPricingTier());
            }
        }, mojo.getPricingTier());
    }

    public String getAppServicePlanName() {
        return mojo.getAppServicePlanName();
    }

    public String getAppServicePlanResourceGroup() {
        return mojo.getAppServicePlanResourceGroup();
    }

    public String getSubscriptionId() {
        return mojo.getSubscriptionId();
    }

    public Region getRegion() {
        return parseExpandableParameter(Region::fromName, mojo.getRegion());
    }

    public DockerConfiguration getDockerConfiguration() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        final OperatingSystem os = getOs(runtime);
        if (os != OperatingSystem.DOCKER) {
            return null;
        }
        final MavenDockerCredentialProvider credentialProvider = getDockerCredential(runtime.getServerId());
        return DockerConfiguration.builder()
                .registryUrl(runtime.getRegistryUrl())
                .image(runtime.getImage())
                .userName(credentialProvider.getUsername())
                .password(credentialProvider.getPassword()).build();
    }

    public List<WebAppArtifact> getMavenArtifacts() throws AzureExecutionException {
        if (mojo.getDeployment() == null || mojo.getDeployment().getResources() == null) {
            return Collections.emptyList();
        }
        return convertResourceToArtifacts(mojo.getDeployment().getResources());
    }

    public Runtime getRuntime() {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null || runtime.isEmpty()) {
            return null;
        }
        final OperatingSystem os = getOs(runtime);
        if (os == OperatingSystem.DOCKER) {
            return Runtime.DOCKER;
        }
        final JavaVersion javaVersion = parseExpandableParameter(JavaVersion::fromString, runtime.getJavaVersion());
        final WebContainer webContainer = parseExpandableParameter(WebContainer::fromString, runtime.getWebContainer());
        return Runtime.getRuntime(os, webContainer, javaVersion);
    }

    private OperatingSystem getOs(final MavenRuntimeConfig runtime) {
        return OperatingSystem.fromString(runtime.getOs());
    }

    public WebAppConfig parse() throws AzureExecutionException {
        return WebAppConfig.builder()
                .subscriptionId(getSubscriptionId())
                .appName(getAppName())
                .resourceGroup(getResourceGroup())
                .servicePlanName(getAppServicePlanName())
                .servicePlanResourceGroup(getAppServicePlanResourceGroup())
                .pricingTier(getPricingTier())
                .region(getRegion())
                .runtime(getRuntime())
                .dockerConfiguration(getDockerConfiguration())
                .deploymentSlotName(getDeploymentSlotName())
                .deploymentSlotConfigurationSource(getDeploymentSlotConfigurationSource())
                .webAppArtifacts(getMavenArtifacts())
                .appSettings(this.mojo.getAppSettings())
                .build();
    }

    // todo: replace WebAppConfiguration with WebAppConfig
    public WebAppConfiguration getWebAppConfiguration() {
        WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = WebAppConfiguration.builder();
        final Runtime runtime = getRuntime();
        final OperatingSystem os = Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).orElse(null);
        if (os == null) {
            Log.debug("No runtime related config is specified. It will cause error if creating a new web app.");
        } else {
            switch (os) {
                case WINDOWS:
                case LINUX:
                    builder = builder.javaVersion(Objects.toString(runtime.getJavaVersion())).webContainer(Objects.toString(runtime.getWebContainer()));
                    break;
                case DOCKER:
                    final MavenRuntimeConfig runtimeConfig = mojo.getRuntime();
                    builder = builder.image(runtimeConfig.getImage()).serverId(runtimeConfig.getServerId()).registryUrl(runtimeConfig.getRegistryUrl());
                    break;
                default:
                    Log.debug("Invalid operating system from the configuration.");
            }
        }
        return builder.appName(getAppName())
                .resourceGroup(getResourceGroup())
                .region(getRegion())
                .pricingTier(mojo.getPricingTier())
                .servicePlanName(mojo.getAppServicePlanName())
                .servicePlanResourceGroup(mojo.getAppServicePlanResourceGroup())
                .deploymentSlotSetting(mojo.getDeploymentSlotSetting())
                .os(os)
                .mavenSettings(mojo.getSettings())
                .resources(Optional.ofNullable(mojo.getDeployment()).map(Deployment::getResources).orElse(null))
                .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
                .buildDirectoryAbsolutePath(mojo.getBuildDirectoryAbsolutePath())
                .project(mojo.getProject())
                .session(mojo.getSession())
                .filtering(mojo.getMavenResourcesFiltering())
                .schemaVersion("v2")
                .build();
    }

    protected MavenDockerCredentialProvider getDockerCredential(String serverId) {
        return MavenDockerCredentialProvider.fromMavenSettings(mojo.getSettings(), serverId);
    }

    protected static List<WebAppArtifact> convertResourceToArtifacts(List<DeploymentResource> resources) throws AzureExecutionException {
        try {
            return CollectionUtils.isEmpty(resources) ? Collections.emptyList() :
                    resources.stream()
                            // filter out external resources
                            .filter(resource -> !resource.isExternalResource())
                            .flatMap(resource -> convertResourceToArtifacts(resource).stream()).collect(Collectors.toList());
        } catch (Throwable ex) {
            throw new AzureExecutionException(String.format("Cannot parse deployment resources due to error: %s.", ex.getMessage()), ex);
        }
    }

    private static <T extends ExpandableParameter> T parseExpandableParameter(Function<String, T> parser, String input) {
        final T result = parser.apply(input);
        if (StringUtils.isNotEmpty(input) && result.isExpandedValue()) {
            AzureMessager.getMessager().warning(String.format("'%s' may not be a valid %s", input, result.getClass().getSimpleName()));
        }
        return result;
    }

    private static List<WebAppArtifact> convertResourceToArtifacts(DeploymentResource resource) throws AzureToolkitRuntimeException {
        final boolean isOneDeploymentResource = resource.isOneDeployResource();
        if (isOneDeploymentResource) {
            return convertOneDeployResourceToArtifacts(resource);
        }
        return convertLegacyResourceToArtifacts(resource);
    }

    private static List<WebAppArtifact> convertLegacyResourceToArtifacts(DeploymentResource resource) {
        final List<File> artifacts = MavenArtifactUtils.getArtifacts(resource);
        return artifacts.stream()
                .map(file -> WebAppArtifact.builder().file(file).deployType(DeployType.getDeployTypeFromFile(file)).path(resource.getTargetPath()).build())
                .collect(Collectors.toList());
    }

    private static List<WebAppArtifact> convertOneDeployResourceToArtifacts(Resource resource) {
        final List<File> artifacts = MavenArtifactUtils.getArtifacts(resource);
        final String typeString = ((DeploymentResource) resource).getType();
        final DeployType type = DeployType.fromString(typeString);
        Objects.requireNonNull(type, () -> String.format("Unsupported resource type '%s', please change to one from this list: %s", typeString,
                DeployType.values().stream().map(Object::toString).map(StringUtils::lowerCase).collect(Collectors.joining(", "))));
        if (type.requireSingleFile() && artifacts.size() > 1) {
            throw new AzureToolkitRuntimeException(String.format("Multiple files are found for resource type('%s'), only one file is allowed.",
                    type));
        }

        if (artifacts.isEmpty()) {
            Log.warn(String.format("Cannot find any files defined by resource(%s)",
                    StringUtils.firstNonBlank(resource.toString())));
        }
        if (type.ignorePath() && StringUtils.isNotBlank(resource.getTargetPath())) {
            throw new AzureToolkitRuntimeException(String.format("'<targetPath>' is not allowed for deployable type('%s').",
                    type));
        }
        if (StringUtils.isNotBlank(type.getFileExt())) {
            final String expectFileExtension = type.getFileExt();
            for (final File file : artifacts) {
                if (!StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(file.getName()), expectFileExtension)) {
                    throw new AzureToolkitRuntimeException(String.format("Wrong file '%s' Deployable type('%s'), expected file type '%s'",
                            file, type, expectFileExtension));
                }
            }
        }
        return artifacts.stream()
                .map(file -> WebAppArtifact.builder().file(file).deployType(type).path(getRemotePath(type, resource, file)).build())
                .collect(Collectors.toList());

    }

    private static String getRemotePath(@Nonnull DeployType type, Resource resource, File file) {
        if (type.ignorePath()) {
            return null;
        }
        if (StringUtils.isNotBlank(resource.getTargetPath()) || type.requirePath()) {
            // remove the prefix like '/' and '/home/site/libs'
            final String toDir = normalizePathString(StringUtils.defaultString(resource.getTargetPath()),
                    type.getTargetPathPrefix(), "/");
            return normalizePath(Paths.get(StringUtils.defaultString(toDir), file.getName()).toString());
        } else {
            return null;
        }
    }

    private static String normalizePathString(String path, String... prefixes) {
        if (StringUtils.isNotBlank(path)) {
            String normalizedPath = StringUtils.trim(normalizePath(path));
            for (final String prefix : prefixes) {
                normalizedPath = StringUtils.removeStart(normalizedPath, prefix);
            }
            return normalizedPath;
        }
        return StringUtils.EMPTY;
    }

    private static String normalizePath(String path) {
        // replace both types of slash
        if (StringUtils.isBlank(path)) {
            return path;
        }
        return StringUtils.removeEnd(path.replaceAll("([\\\\/])+", Matcher.quoteReplacement("/")), "/");
    }

}
