/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import java.util.Collections;
import java.util.List;

public class V2ConfigParser extends AbstractConfigParser {
    public V2ConfigParser(AbstractWebAppMojo mojo, AbstractConfigurationValidator validator) {
        super(mojo, validator);
    }

    @Override
    public Region getRegion() throws AzureExecutionException {
        validate(validator::validateRegion);
        return Region.fromName(mojo.getRegion());
    }

    @Override
    public DockerConfiguration getDockerConfiguration() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        final OperatingSystem os = getOs(runtime);
        if (os != OperatingSystem.DOCKER) {
            return null;
        }
        validate(validator::validateImage);
        final MavenDockerCredentialProvider credentialProvider = getDockerCredential(runtime.getServerId());
        return DockerConfiguration.builder()
                .registryUrl(runtime.getRegistryUrl())
                .image(runtime.getImage())
                .userName(credentialProvider.getUsername())
                .password(credentialProvider.getPassword()).build();
    }

    @Override
    public List<WebAppArtifact> getMavenArtifacts() throws AzureExecutionException {
        if (mojo.getDeployment() == null || mojo.getDeployment().getResources() == null) {
            return Collections.EMPTY_LIST;
        }
        return parseArtifactsFromResources(mojo.getDeployment().getResources());
    }

    @Override
    public Runtime getRuntime() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null || runtime.isEmpty()) {
            return null;
        }
        final OperatingSystem os = getOs(runtime);
        if (os == OperatingSystem.DOCKER) {
            return Runtime.DOCKER;
        }
        validate(validator::validateJavaVersion);
        validate(validator::validateWebContainer);
        validate(validator::validateRuntimeStack);
        final JavaVersion javaVersion = JavaVersion.fromString(runtime.getJavaVersionRaw());
        final WebContainer webContainer = WebContainer.fromString(runtime.getWebContainerRaw());
        return Runtime.getRuntime(os, webContainer, javaVersion);
    }

    private OperatingSystem getOs(final MavenRuntimeConfig runtime) throws AzureExecutionException {
        validate(validator::validateOs);
        return OperatingSystem.fromString(runtime.getOs());
    }
}
