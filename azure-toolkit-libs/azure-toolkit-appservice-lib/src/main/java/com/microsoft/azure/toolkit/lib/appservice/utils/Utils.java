/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.utils;

import com.azure.resourcemanager.appservice.models.AppSetting;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
    private static final String SUBSCRIPTIONS = "subscriptions";

    public static Map<String, String> normalizeAppSettings(Map<String, AppSetting> input) {
        return input.entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().value()));
    }

    public static String getSegment(String id, String segment) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        final String[] attributes = StringUtils.lowerCase(id).split("/");
        final int pos = ArrayUtils.indexOf(attributes, StringUtils.lowerCase(segment));
        if (pos >= 0) {
            return attributes[pos + 1];
        }
        return null;
    }

    public static String getSubscriptionId(String resourceId) {
        return getSegment(resourceId, SUBSCRIPTIONS);
    }

    public static DeployType getDeployTypeByFileExtension(File file) {
        final String fileExtensionName = FilenameUtils.getExtension(file.getName());
        switch (StringUtils.lowerCase(fileExtensionName)) {
            case "jar":
                return DeployType.JAR;
            case "war":
                return DeployType.WAR;
            case "ear":
                return DeployType.EAR;
            case "zip":
                return DeployType.ZIP;
            default:
                throw new AzureToolkitRuntimeException("Unsupported file type, please set the deploy type.");
        }
    }
}
