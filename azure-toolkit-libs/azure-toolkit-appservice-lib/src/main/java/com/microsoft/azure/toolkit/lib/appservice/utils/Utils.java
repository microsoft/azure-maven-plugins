/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.utils;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.resourcemanager.appservice.models.AppSetting;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Utils {

    public static Map<String, String> normalizeAppSettings(Map<String, AppSetting> input) {
        return input.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().value()));
    }

    public static List<HttpPipelinePolicy> getPolicyFromPipeline(@Nonnull final HttpPipeline httpPipeline,
                                                                 @Nullable final Predicate<HttpPipelinePolicy> filter) {
        final List<HttpPipelinePolicy> policies = new ArrayList<>();
        for (int i = 0, count = httpPipeline.getPolicyCount(); i < count; ++i) {
            final HttpPipelinePolicy policy = httpPipeline.getPolicy(i);
            if (filter == null || filter.test(policy)) {
                policies.add(policy);
            }
        }
        return policies;
    }

    public static String getSubscriptionId(String resourceId) {
        return ResourceId.fromString(resourceId).subscriptionId();
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

    public static <T> T selectFirstOptionIfCurrentInvalid(String name, List<T> options, T value) {
        if (options.isEmpty()) {
            throw new AzureToolkitRuntimeException(String.format("No %s is available.", name));
        }
        return options.contains(value) ? value : options.get(0);
    }

    public static <T> void mergeObjects(T to, T from) throws IllegalAccessException {
        for (Field field : FieldUtils.getAllFields(from.getClass())) {
            if (FieldUtils.readField(field, to, true) == null) {
                final Object value = FieldUtils.readField(field, from, true);
                if (value != null) {
                    FieldUtils.writeField(field, to, value, true);
                }
            }
        }
    }

    public static void throwForbidCreateResourceWarning(String resourceType, String name) {
        throw new AzureToolkitRuntimeException(String.format("%s(%s) cannot be found, if you want to create azure resources please remove command line arguments: " +
            "`-Dazure.resource.create.skip=true` or `-DskipCreateAzureResource`.", resourceType, name));
    }
}
