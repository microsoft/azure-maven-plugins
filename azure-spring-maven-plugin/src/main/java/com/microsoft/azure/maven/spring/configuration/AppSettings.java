/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;

import java.util.LinkedHashMap;
import java.util.Map;

public class AppSettings extends BaseSettings {
    private String subscriptionId;
    private String clusterName;
    private String appName;
    private String isPublic;

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

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

    public String isPublic() {
        return isPublic;
    }

    public void setPublic(String isPublic) {
        this.isPublic = isPublic;
    }

    @Override
    protected Map<String, Object> getProperties() {
        return MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[] {
            new DefaultMapEntry<>("subscriptionId", this.subscriptionId),
            new DefaultMapEntry<>("clusterName", this.clusterName),
            new DefaultMapEntry<>("appName", this.appName),
            new DefaultMapEntry<>("isPublic", this.isPublic)
        });
    }

}
