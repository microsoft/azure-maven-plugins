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
    public static final String TELEMETRY_KEY_SUBSCRIPTION_ID = "subscriptionId";
    public static final String TELEMETRY_KEY_JAVA_VERSION = "javaVersion";

    /**
     *  Whether user modify their pom file with azure-spring:config
     */
    public static final String TELEMETRY_KEY_POM_FILE_MODIFIED = "isPomFileModified";

    public static final String TELEMETRY_KEY_AUTH_METHOD = "authMethod";
    public static final String TELEMETRY_KEY_IS_SERVICE_PRINCIPAL = "isServicePrincipal";
    public static final String TELEMETRY_KEY_IS_KEY_ENCRYPTED = "isKeyEncrypted";
    public static final String TELEMETRY_KEY_IS_CREATE_NEW_APP = "isCreateNewApp";
    public static final String TELEMETRY_KEY_IS_CREATE_DEPLOYMENT = "isCreateDeployment";
    public static final String TELEMETRY_KEY_IS_DEPLOYMENT_NAME_GIVEN = "isDeploymentNameGiven";

    public static final String TELEMETRY_VALUE_AUTH_POM_CONFIGURATION = "Pom Configuration";
    public static final String TELEMETRY_VALUE_AUTH_MAVEN_SERVER = "Maven Server";
    public static final String TELEMETRY_VALUE_AUTH_AZURE_MAVEN_PLUGIN = "Azure Maven Plugin";
    public static final String TELEMETRY_VALUE_AUTH_AZURE_CLI = "Azure CLI";
    public static final String TELEMETRY_KEY_INSTALLATIONID = "installationId";
    public static final String TELEMETRY_KEY_SESSION_ID = "sessionId";
    public static final String TELEMETRY_KEY_PLUGIN_NAME = "pluginName";
    public static final String TELEMETRY_KEY_PLUGIN_VERSION = "pluginVersion";
    public static final String TELEMETRY_KEY_ERROR_CODE = "errorCode";
    public static final String TELEMETRY_KEY_ERROR_TYPE = "errorType";
    public static final String TELEMETRY_KEY_ERROR_MESSAGE = "errorMessage";
    public static final String TELEMETRY_KEY_DURATION = "duration";
    public static final String TELEMETRY_VALUE_USER_ERROR = "userError";
    public static final String TELEMETRY_VALUE_SYSTEM_ERROR = "systemError";
    public static final String TELEMETRY_EVENT_TELEMETRY_NOT_ALLOWED = "TelemetryNotAllowed";
    public static final String TELEMETRY_VALUE_ERROR_CODE_SUCCESS = "0";
    public static final String TELEMETRY_VALUE_ERROR_CODE_FAILURE = "1";

    private TelemetryConstants() {

    }
}
