/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WebContainerUtils {
    public static String formatWebContainer(WebContainer webContainer) {
        return Objects.toString(webContainer, null);
    }

    public static List<String> getAvailableWebContainer(@Nonnull OperatingSystem os, @Nonnull JavaVersion javaVersion, boolean isJarPacking) {
        final List<String> result = new ArrayList<>();
        if (isJarPacking) {
            result.add(WebContainer.JAVA_SE.toString());
        } else {
            for (final Runtime runtime : Azure.az(AzureAppService.class).listWebAppRuntimes(os, javaVersion)) {
                result.add(runtime.getWebContainer().toString());
            }
            result.remove(WebContainer.JAVA_SE.toString());
        }

        Collections.sort(result);
        return result;
    }
}
