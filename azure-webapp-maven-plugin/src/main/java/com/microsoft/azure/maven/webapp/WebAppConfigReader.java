/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.maven.webapp.parser.AbstractConfigParser;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
@SuperBuilder(toBuilder = true)
public class WebAppConfigReader {
    private AbstractWebAppMojo mojo;
    private AbstractConfigParser parser;
    private AbstractConfigurationValidator validator;
    private boolean showWarning = false;
    private boolean breakOnError = true;
    private boolean useDefaultValue = true;

    public WebAppConfig getWebAppConfig() throws AzureExecutionException {
        return WebAppConfig.builder()
                .appName(getProperty(parser::getAppName, validator::validateAppName))
                .resourceGroup(getProperty(parser::getResourceGroup, validator::validateResourceGroup))
                .servicePlanName(parser.getAppServicePlanName())
                .servicePlanResourceGroup(parser.getAppServicePlanResourceGroup())
                .pricingTier(getProperty(parser::getPricingTier, validator::validatePricingTier))
                .region(getProperty(parser::getRegion, validator::validateRegion))
                .runtime(getRuntime())
                .dockerConfiguration(getDockerConfiguration())
                .deploymentSlotName(parser.getDeploymentSlotName())
                .deploymentSlotConfigurationSource(parser.getDeploymentSlotConfigurationSource())
                .resources(parser.getResources())
                .schemaVersion(parser.getSchemaVersion())
                .stagingDirectoryPath(parser.getStagingDirectoryPath())
                .buildDirectoryAbsolutePath(parser.getBuildDirectoryAbsolutePath())
                .project(parser.getProject())
                .session(parser.getSession())
                .filtering(parser.getResourcesFiltering())
                .build();
    }

    private DockerConfiguration getDockerConfiguration() throws AzureExecutionException {
        if (mojo.getRuntime() == null || !StringUtils.equalsIgnoreCase(mojo.getRuntime().getOs(), "docker")) {
            return null;
        }
        return getProperty(parser::getDockerConfiguration, validator::validateImage);
    }

    private Runtime getRuntime() throws AzureExecutionException {
        if (mojo.getRuntime() == null) {
            return useDefaultValue ? Runtime.LINUX_JAVA8_TOMCAT85 : null;
        }
        final List<String> messages = new ArrayList<>();
        messages.add(validator.validateOs());
        if (StringUtils.equalsIgnoreCase(mojo.getRuntime().getOs(), "windows")) {
            messages.add(validator.validateJavaVersion());
            messages.add(validator.validateWebContainer());
        }
        if (StringUtils.equalsIgnoreCase(mojo.getRuntime().getOs(), "linux")) {
            messages.add(validator.validateRuntimeStack());
        }
        return getProperty(() -> parser.getRuntime(), () -> String.join(StringUtils.LF, messages));
    }

    private <T> T getProperty(Supplier<T> parseFunction, Supplier<String> validateFunction) throws AzureExecutionException {
        final String errorMessage = validateFunction == null ? null : validateFunction.get();
        if (StringUtils.isNotEmpty(errorMessage)) {
            if (showWarning) {
                Log.warn(errorMessage);
            }
            if (breakOnError) {
                throw new AzureExecutionException(errorMessage);
            }
        }
        return parseFunction.get();
    }
}
