/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import org.codehaus.plexus.util.StringUtils;

import java.net.URL;

/**
 * Docker container setting class.
 */
public class ContainerSetting {
    /**
     * Docker image name
     */
    private String imageName;

    /**
     * Start up file.
     */
    private String startUpFile;

    /**
     * Server Id.
     */
    private String serverId;

    /**
     * Private registry URL.
     */
    private URL registryUrl;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getStartUpFile() {
        return startUpFile;
    }

    public void setStartUpFile(String startUpFile) {
        this.startUpFile = startUpFile;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getRegistryUrl() {
        return registryUrl == null ? null : registryUrl.toString();
    }

    public void setRegistryUrl(URL registryUrl) {
        this.registryUrl = registryUrl;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(getImageName()) &&
                StringUtils.isEmpty(getStartUpFile()) &&
                StringUtils.isEmpty(getServerId()) &&
                StringUtils.isEmpty(getRegistryUrl());
    }
}
