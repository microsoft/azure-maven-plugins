/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.servicefabric;

public class Constants{

    public static final String DEFAULT_SCHEMA_VERSION = "1.0.0-preview2";
    public static final String SERVICE_FABRIC_RESOURCES_PATH = "servicefabric";
    public static final String APPLICATION_RESOURCE_NAME = "app.yaml";
    public static final String SERVICE_RESOURCE_NAME = "service.yaml";
    public static final String VOLUME_RESOURCE_NAME = "volume.yaml";
    public static final String NETWORK_RESOURCE_NAME = "network.yaml";
    public static final String GATEWAY_RESOURCE_NAME = "gateway.yaml";
    public static final String SECRET_RESOURCE_NAME = "secret.yaml";
    public static final String SECRET_VALUE_RESOURCE_NAME = "secretvalue.yaml";
    public static final String DEFAULT_APPLICATION_DESCRIPTION = "This application has no description";
    public static final String DEFAULT_SERVICE_DESCRIPTION = "This service has no description";
    public static final String DEFAULT_VOLUME_DESCRIPTION = "This volume has no description";
    public static final String DEFAULT_NETWORK_DESCRIPTION = "This network has no description";
    public static final String DEFAULT_GATEWAY_DESCRIPTION = "This gateway has no description";
    public static final String DEFAULT_SECRET_DESCRIPTION = "This secret has no description";
    public static final String DEFAULT_CODE_PACKAGE_NAME = "NO_CODE_PACKAGE";
    public static final String DEFAULT_LISTENER_NAME = "NO_LISTENER";
    public static final String DEFAULT_OS = "CURRENT_SYSTEM_OS";
    public static final String LINUX_OS = "Linux";
    public static final String WINDOWS_OS = "Windows";
    public static final String DEFAULT_CPU_USAGE = "0.5";
    public static final String DEFAULT_MEMORY_USAGE = "0.5";
    public static final String DEFAULT_REPLICA_COUNT = "1";
    public static final String LOCAL_DEPLOYMENT_TYPE = "local";
    public static final String MESH_DEPLOYMENT_TYPE = "mesh";
    public static final String SFRP_DEPLOYMENT_TYPE = "sfrp";
    public static final String DEFAULT_CLUSTER_ENDPOINT = "http://localhost:19080";
    public static final String DEFAULT_VOLUME_PROVIDER = "sfAzureFile";
    public static final String DEFAULT_RESOURCE_GROUP = "NO_RESOURCE_GROUP";
    public static final String DEFAULT_TCP_NAME = "NO_TCP_NAME";
    public static final String DEFAULT_LOCATION = "westus";
    public static final String DEFAULT_DELETE_RESOURCE_GROUP = "false";
    public static final String DEFAULT_NETWORK_KIND = "Local";
    public static final String DEFAULT_SECRET_KIND = "inlinedValue";
    public static final String DEFAULT_ENVIRONMENTAL_VARIABLES = "NO_ENVIRONMENTAL_VARS";
    public static final String DEFAULT_PEM_FILE_PATH = "NO_PEM_PATH";
    public static final String DEFAULT_SECRET_CONTENT_TYPE = "text/plain";
}
