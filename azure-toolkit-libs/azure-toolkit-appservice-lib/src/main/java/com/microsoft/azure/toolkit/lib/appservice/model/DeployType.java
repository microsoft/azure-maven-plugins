/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class DeployType {
    public static final DeployType WAR = new DeployType("war");
    public static final DeployType JAR = new DeployType("jar");
    public static final DeployType EAR = new DeployType("ear");
    public static final DeployType JAR_LIB = new DeployType("lib");
    public static final DeployType STATIC = new DeployType("static");
    public static final DeployType SCRIPT_STARTUP = new DeployType("startup");
    public static final DeployType ZIP = new DeployType("zip");

    private static final Map<DeployType, String> TYPE_TO_TARGET_DIRECTORY_MAP = new HashMap<>();
    private static final Map<DeployType, String> DEPLOY_TYPE_TO_FILE_EXTENSION_MAP = new HashMap<>();

    private static final String WWWROOT = "/home/site/wwwroot/";

    static {
        TYPE_TO_TARGET_DIRECTORY_MAP.put(DeployType.JAR, WWWROOT);
        TYPE_TO_TARGET_DIRECTORY_MAP.put(DeployType.EAR, WWWROOT);
        TYPE_TO_TARGET_DIRECTORY_MAP.put(DeployType.STATIC, WWWROOT);
        TYPE_TO_TARGET_DIRECTORY_MAP.put(DeployType.ZIP, WWWROOT);
        TYPE_TO_TARGET_DIRECTORY_MAP.put(DeployType.WAR, WWWROOT + "/webapps/");
        TYPE_TO_TARGET_DIRECTORY_MAP.put(DeployType.SCRIPT_STARTUP, "/home/site/scripts/");
        TYPE_TO_TARGET_DIRECTORY_MAP.put(DeployType.JAR_LIB, "/home/site/libs/");
    }

    static {
        DEPLOY_TYPE_TO_FILE_EXTENSION_MAP.put(DeployType.JAR, "jar");
        DEPLOY_TYPE_TO_FILE_EXTENSION_MAP.put(DeployType.EAR, "ear");
        DEPLOY_TYPE_TO_FILE_EXTENSION_MAP.put(DeployType.WAR, "war");
        DEPLOY_TYPE_TO_FILE_EXTENSION_MAP.put(DeployType.JAR_LIB, "jar");
        DEPLOY_TYPE_TO_FILE_EXTENSION_MAP.put(DeployType.ZIP, "zip");
    }

    private final String value;

    public static List<DeployType> values() {
        return Arrays.asList(WAR, JAR, EAR, JAR_LIB, STATIC, SCRIPT_STARTUP, ZIP);
    }

    public static DeployType fromString(String input) {
        return values().stream()
            .filter(deployType -> StringUtils.equalsIgnoreCase(deployType.getValue(), input))
            .findFirst().orElse(null);
    }

    public String getFileExt() {
        return DEPLOY_TYPE_TO_FILE_EXTENSION_MAP.get(this);
    }

    public boolean requireSingleFile() {
        return Arrays.asList(DeployType.WAR, DeployType.JAR, DeployType.EAR, DeployType.SCRIPT_STARTUP, DeployType.ZIP).contains(this);
    }

    public boolean requirePath() {
        return Arrays.asList(DeployType.JAR_LIB, DeployType.STATIC).contains(this);
    }

    public boolean ignorePath() {
        return Arrays.asList(DeployType.JAR, DeployType.EAR, DeployType.SCRIPT_STARTUP).contains(this);
    }

    public String getTargetPathPrefix() {
        return TYPE_TO_TARGET_DIRECTORY_MAP.get(this);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
