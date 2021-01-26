/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.ProjectUtils;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.JavaVersion;
import com.microsoft.azure.toolkits.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.WebContainer;
import com.microsoft.azure.tools.common.model.Region;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class V1ConfigParser extends AbstractConfigParser {
    public V1ConfigParser(AbstractWebAppMojo mojo, AbstractConfigurationValidator validator) {
        super(mojo, validator);
    }

    @Override
    public Region getRegion() throws AzureExecutionException {
        validate(validator::validateRegion);
        if (StringUtils.isEmpty(mojo.getRegion())) {
            return Region.EUROPE_WEST;
        }
        return Region.fromName(mojo.getRegion());
    }

    @Override
    public DockerConfiguration getDockerConfiguration() throws AzureExecutionException {
        final OperatingSystem os = getOs();
        if (os != OperatingSystem.DOCKER) {
            return null;
        }
        validate(validator::validateImage);
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final MavenDockerCredentialProvider credentialProvider = getDockerCredential(containerSetting.getServerId());
        return DockerConfiguration.builder()
                .registryUrl(containerSetting.getRegistryUrl())
                .image(containerSetting.getImageName())
                .userName(credentialProvider.getUsername())
                .password(credentialProvider.getPassword())
                .build();
    }

    @Override
    public List<Pair<File, DeployType>> getResources() throws AzureExecutionException {
        switch (mojo.getDeploymentType()) {
            case JAR:
                return Arrays.asList(Pair.of(getFileToDeploy(mojo.getJarFile()), DeployType.JAR));
            case WAR:
                return Arrays.asList(Pair.of(getFileToDeploy(mojo.getWarFile()), DeployType.WAR));
            default:
                final List<File> files = MavenArtifactUtils.getArtifacts(mojo.getResources());
                return files.stream().map(file -> Pair.of(file, getDeployTypeFromFile(file))).collect(Collectors.toList());
        }
    }

    @Override
    public Runtime getRuntime() throws AzureExecutionException {
        final OperatingSystem os = getOs();
        switch (os) {
            case WINDOWS:
                return getRuntimeForWindows();
            case LINUX:
                return getRuntimeForLinux();
            case DOCKER:
                return Runtime.DOCKER;
            default:
                return null;
        }
    }

    private Runtime getRuntimeForWindows() throws AzureExecutionException {
        validate(validator::validateJavaVersion);
        validate(validator::validateWebContainer);
        final WebContainer webContainer = WebContainer.fromString(mojo.getJavaWebContainer().toString());
        final JavaVersion javaVersion = JavaVersion.fromString(mojo.getJavaVersion());
        return Runtime.getRuntime(OperatingSystem.WINDOWS, webContainer, javaVersion);
    }

    private Runtime getRuntimeForLinux() throws AzureExecutionException {
        validate(validator::validateRuntimeStack);
        return Runtime.getRuntimeFromLinuxFxVersion(mojo.getLinuxRuntime());
    }

    private OperatingSystem getOs() throws AzureExecutionException {
        validate(validator::validateOs);
        final String linuxRuntime = mojo.getLinuxRuntime();
        final String javaVersion = mojo.getJavaVersion();
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final boolean isContainerSettingEmpty = containerSetting == null || containerSetting.isEmpty();
        final List<OperatingSystem> osList = new ArrayList<>();

        if (javaVersion != null) {
            osList.add(OperatingSystem.WINDOWS);
        }
        if (linuxRuntime != null) {
            osList.add(OperatingSystem.LINUX);
        }
        if (!isContainerSettingEmpty) {
            osList.add(OperatingSystem.DOCKER);
        }
        return osList.size() > 0 ? osList.get(0) : null;
    }

    private File getFileToDeploy(String filePath) {
        final IProject project = ProjectUtils.convertCommonProject(mojo.getProject());
        return StringUtils.isEmpty(filePath) ? project.getArtifactFile().toFile() : new File(filePath);
    }
}
