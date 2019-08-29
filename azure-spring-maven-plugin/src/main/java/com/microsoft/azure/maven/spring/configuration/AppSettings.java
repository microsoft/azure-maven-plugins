/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import com.microsoft.azure.maven.spring.utils.XmlUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class AppSettings {
    private String clusterName;
    private String appName;
    private String runtimeVersion;
    private String isPublic;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public String isPublic() {
        return isPublic;
    }

    public void setIsPublic(String isPublic) {
        this.isPublic = isPublic;
    }

    public void applyToXpp3Dom(Xpp3Dom deployment) {
        XmlUtils.replaceDomWithKeyValue(deployment, "clusterName", this.clusterName);
        XmlUtils.replaceDomWithKeyValue(deployment, "appName", this.appName);
        XmlUtils.replaceDomWithKeyValue(deployment, "isPublic", this.isPublic);
        XmlUtils.replaceDomWithKeyValue(deployment, "runtimeVersion", this.runtimeVersion);
    }
}
