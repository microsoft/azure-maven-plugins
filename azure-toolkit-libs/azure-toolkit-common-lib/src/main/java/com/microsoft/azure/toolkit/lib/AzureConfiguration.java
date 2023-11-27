/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class AzureConfiguration {
    public static final List<String> DEFAULT_DOCUMENT_LABEL_FIELDS = Arrays.asList("name", "Name", "NAME", "ID", "UUID", "Id", "id", "uuid");

    private String logLevel;
    private String userAgent;
    private String cloud;
    private String machineId;
    private String product;
    private String version;
    private String sessionId;
    private String databasePasswordSaveType;
    private Boolean telemetryEnabled; // null means true
    private String functionCoreToolsPath;
    private String azureCliPath;
    private String dotnetRuntimePath;
    private String storageExplorerPath;
    private String proxySource;
    private String httpProxyHost;
    private int httpProxyPort;
    private String proxyUsername;
    private String proxyPassword;
    @Nullable
    private String nonProxyHosts;
    @Nullable
    @JsonIgnore
    private SSLContext sslContext;
    private int pageSize = 99;
    private List<String> documentsLabelFields = new ArrayList<>(DEFAULT_DOCUMENT_LABEL_FIELDS);
    private int monitorQueryRowNumber = 200;
    private boolean authPersistenceEnabled = true;
    private String eventHubsConsumerGroup = "$Default";

    private String azuritePath;
    private String azuriteWorkspace;
    private Boolean enableLeaseMode = false;

    private Boolean enablePreloading = false;

    public void setProxyInfo(ProxyInfo proxy) {
        this.setProxySource(proxy.getSource());
        this.setHttpProxyHost(proxy.getHost());
        this.setHttpProxyPort(proxy.getPort());
        this.setProxyUsername(proxy.getUsername());
        this.setProxyPassword(proxy.getPassword());
        this.setNonProxyHosts(proxy.getNonProxyHosts());
    }
}
