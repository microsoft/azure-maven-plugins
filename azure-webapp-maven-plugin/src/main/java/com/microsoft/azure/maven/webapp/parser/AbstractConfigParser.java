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
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
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
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public abstract class AbstractConfigParser {

    protected AbstractWebAppMojo mojo;

    public AbstractConfigParser(AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    public String getAppName() throws AzureExecutionException {
        return mojo.getAppName();
    }

    public String getResourceGroup() throws AzureExecutionException {
        return mojo.getResourceGroup();
    }

    public String getDeploymentSlotName() {
        return mojo.getDeploymentSlotSetting() == null ? null : mojo.getDeploymentSlotSetting().getName();
    }

    public String getDeploymentSlotConfigurationSource() {
        return mojo.getDeploymentSlotSetting() == null ? null : mojo.getDeploymentSlotSetting().getConfigurationSource();
    }

    public PricingTier getPricingTier() throws AzureExecutionException {
        return PricingTier.fromString(mojo.getPricingTier());
    }

    public String getAppServicePlanName() throws AzureExecutionException {
        return mojo.getAppServicePlanName();
    }

    public String getAppServicePlanResourceGroup() throws AzureExecutionException {
        return mojo.getAppServicePlanResourceGroup();
    }

    public String getSubscriptionId() {
        return mojo.getSubscriptionId();
    }

    public abstract Region getRegion() throws AzureExecutionException;

    public abstract DockerConfiguration getDockerConfiguration() throws AzureExecutionException;

    public abstract List<WebAppArtifact> getMavenArtifacts() throws AzureExecutionException;

    public abstract Runtime getRuntime() throws AzureExecutionException;

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

    protected MavenDockerCredentialProvider getDockerCredential(String serverId) {
        return MavenDockerCredentialProvider.fromMavenSettings(mojo.getSettings(), serverId);
    }

    protected void validate(Supplier<String> validator) throws AzureExecutionException {
        final String message = validator.get();
        if (StringUtils.isNotEmpty(message)) {
            throw new AzureExecutionException(message);
        }
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
