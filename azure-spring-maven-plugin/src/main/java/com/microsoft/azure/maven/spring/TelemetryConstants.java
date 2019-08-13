/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

public class TelemetryConstants {
    public static final String TELEMETRY_KEY_PUBLIC = "public";
    public static final String TELEMETRY_KEY_RUNTIME_VERSION = "runtimeVersion";
    public static final String TELEMETRY_KEY_CPU = "cpu";
    public static final String TELEMETRY_KEY_MEMORY = "memory";
    public static final String TELEMETRY_KEY_INSTANCE_COUNT = "instanceCount";
    public static final String TELEMETRY_KEY_JVM_OPTIONS = "jvmOptions";
    public static final String TELEMETRY_KEY_WITHIN_PARENT_POM = "isExecutedWithinParentPom";

    public static final String TELEMETRY_KEY_AUTH_METHOD = "authMethod";
    public static final String TELEMETRY_KEY_IS_SERVICE_PRINCIPAL = "isServicePrincipal";
    public static final String TELEMETRY_KEY_IS_KEY_ENCRYPTED = "isKeyEncrypted";
    public static final String TELEMETRY_KEY_IS_CREATE_NEW_APP = "isCreateNewApp";
    public static final String TELEMETRY_KEY_IS_UPDATE_CONFIGURATION = "isUpdateConfiguration";

    public static final String TELEMETRY_VALUE_AUTH_POM_CONFIGURATION = "Pom Configuration";
    public static final String TELEMETRY_VALUE_AUTH_MAVEN_SERVER = "Maven Server";
    public static final String TELEMETRY_VALUE_AUTH_AZURE_MAVEN_PLUGIN = "Azure Maven Plugin";
    public static final String TELEMETRY_VALUE_AUTH_AZURE_CLI = "Azure CLI";

    private TelemetryConstants() {

    }
}
