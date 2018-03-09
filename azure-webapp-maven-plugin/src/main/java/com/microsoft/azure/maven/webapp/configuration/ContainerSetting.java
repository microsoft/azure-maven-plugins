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
     * Whether to use built-in blessed image
     */
    private boolean useBuiltinImage;

    /**
     * Image name used for Web App on Linux or Web App for container.<br/>
     * Below is the list of supported built-in image:
     * <ul>
     *     <li>tomcat 8.5-jre8</li>
     *     <li>tomcat 9.0-jre8</li>
     * </ul>
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

    public boolean isUseBuiltinImage() {
        return useBuiltinImage;
    }

    public void setUseBuiltinImage(boolean useBuiltinImage) {
        this.useBuiltinImage = useBuiltinImage;
    }
}
