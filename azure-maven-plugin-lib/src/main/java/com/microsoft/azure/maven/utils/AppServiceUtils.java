/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import com.google.common.io.CharStreams;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.CsmPublishingProfileOptions;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.appservice.DockerImageType;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppServiceUtils {

    private static final String SERVICE_PLAN_NOT_FOUND = "Failed to get App Service Plan";
    private static final String UPDATE_APP_SERVICE_PLAN = "Updating app service plan";
    private static final List<PricingTier> pricingTiers = new ArrayList<>();
    private static final Pattern FTP_REGEX = Pattern.compile("publishMethod=\"FTP\" publishUrl=\"ftp://([^\"]+).+" +
            "userName=\"([^\"]+\\\\\\$[^\"]+)\".+userPWD=\"([^\"]+)\"");
    public static final String FAILED_TO_GET_PUBLISHING_PROFILE = "Failed to get publishing profile of deployment slot %s";
    public static final String FAILED_TO_PARSE_PUBLISHING_PROFILE = "Failed to parse publishing profile\\n %s";

    static {
        // Init runtimeStack list
        for (final Field field : PricingTier.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    pricingTiers.add((PricingTier) field.get(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static AppServicePlan getAppServicePlan(final String servicePlanName, final Azure azureClient,
                                                   final String resourceGroup, final String servicePlanResourceGroup) {
        if (StringUtils.isNotEmpty(servicePlanName)) {
            final String servicePlanResGrp = getAppServicePlanResourceGroup(resourceGroup, servicePlanResourceGroup);
            return azureClient.appServices().appServicePlans()
                .getByResourceGroup(servicePlanResGrp, servicePlanName);
        }
        return null;
    }

    public static String getAppServicePlanResourceGroup(final String resourceGroup, final String appServicePlanResGrp) {
        return StringUtils.isEmpty(appServicePlanResGrp) ? resourceGroup : appServicePlanResGrp;
    }

    public static String getAppServicePlanName(final String servicePlanName) {
        return StringUtils.isEmpty(servicePlanName) ? generateRandomServicePlanName() : servicePlanName;
    }

    private static String generateRandomServicePlanName() {
        return "ServicePlan" + UUID.randomUUID().toString().substring(0, 18);
    }

    public static PricingTier getPricingTierFromString(final String pricingTierString) {
        for (final PricingTier pricingTier : pricingTiers) {
            if (pricingTier.toSkuDescription().size().equalsIgnoreCase(pricingTierString)) {
                return pricingTier;
            }
        }
        return null;
    }

    public static String convertPricingTierToString(final PricingTier pricingTier) {
        return pricingTier == null ? null : pricingTier.toSkuDescription().size();
    }

    public static List<PricingTier> getAvailablePricingTiers(OperatingSystem operatingSystem) {
        // This is a workaround for https://github.com/Azure/azure-libraries-for-java/issues/660
        // Linux app service didn't support P1,P2,P3 pricing tier.
        final List<PricingTier> result = new ArrayList<>(pricingTiers);
        if (operatingSystem == OperatingSystem.LINUX) {
            result.remove(PricingTier.PREMIUM_P1);
            result.remove(PricingTier.PREMIUM_P2);
            result.remove(PricingTier.PREMIUM_P3);
        }
        return result;
    }

    public static AppServicePlan getAppServicePlanByAppService(final WebAppBase webApp) {
        return webApp.manager().appServicePlans().getById(webApp.appServicePlanId());
    }

    public static AppServicePlan updateAppServicePlan(final AppServicePlan appServicePlan,
                                                      final PricingTier pricingTier) throws AzureExecutionException {
        if (appServicePlan == null) {
            throw new AzureExecutionException(SERVICE_PLAN_NOT_FOUND);
        }
        Log.info(UPDATE_APP_SERVICE_PLAN);
        final AppServicePlan.Update appServicePlanUpdate = appServicePlan.update();
        // Update pricing tier
        if (pricingTier != null && !appServicePlan.pricingTier().equals(pricingTier)) {
            appServicePlanUpdate.withPricingTier(pricingTier);
        }
        return appServicePlanUpdate.apply();
    }

    public static boolean isEqualAppServicePlan(AppServicePlan first, AppServicePlan second) {
        return first == null ? second == null : second != null && StringUtils.equals(first.id(), second.id());
    }

    public static DockerImageType getDockerImageType(final String imageName, final String serverId,
                                                     final String registryUrl) {
        if (StringUtils.isEmpty(imageName)) {
            return DockerImageType.NONE;
        }

        final boolean isCustomRegistry = StringUtils.isNotEmpty(registryUrl);
        final boolean isPrivate = StringUtils.isNotEmpty(serverId);

        if (isCustomRegistry) {
            return isPrivate ? DockerImageType.PRIVATE_REGISTRY : DockerImageType.UNKNOWN;
        } else {
            return isPrivate ? DockerImageType.PRIVATE_DOCKER_HUB : DockerImageType.PUBLIC_DOCKER_HUB;
        }
    }

    public static PublishingProfile getPublishingProfileFromDeploymentTarget(DeployTarget deployTarget) throws AzureExecutionException {
        if (!StringUtils.equalsIgnoreCase(deployTarget.getType(), DeployTargetType.SLOT.toString())) {
            return deployTarget.getPublishingProfile();
        }
        // Below is the workaround for https://github.com/Azure/azure-libraries-for-java/issues/955
        // Invoke listPublishingProfileXmlWithSecretsSlotAsync directly and convert the xml to PublishingProfile
        final DeploymentSlot targetWebApp = (DeploymentSlot) deployTarget.getApp();
        final InputStream profileStream = deployTarget.getApp().manager().inner().webApps()
                .listPublishingProfileXmlWithSecretsSlotAsync(targetWebApp.resourceGroupName(), targetWebApp.parent().name(),
                        targetWebApp.name(), new CsmPublishingProfileOptions()).toBlocking().single();
        try {
            final String xml = CharStreams.toString(new InputStreamReader(profileStream));
            return parseFTPPublishingProfileFromXml(xml);
        } catch (IOException e) {
            throw new AzureExecutionException(String.format(FAILED_TO_GET_PUBLISHING_PROFILE, targetWebApp.name()), e);
        }
    }

    public static PublishingProfile parseFTPPublishingProfileFromXml(String publishingProfileXml) throws AzureExecutionException {
        final Matcher matcher = FTP_REGEX.matcher(publishingProfileXml);
        if (matcher.find()) {
            final String ftpUrl = matcher.group(1);
            final String ftpUsername = matcher.group(2);
            final String ftpPassword = matcher.group(3);
            return new PublishingProfile() {
                @Override
                public String ftpUrl() {
                    return ftpUrl;
                }

                @Override
                public String ftpUsername() {
                    return ftpUsername;
                }

                @Override
                public String ftpPassword() {
                    return ftpPassword;
                }

                @Override
                public String gitUrl() {
                    return null;
                }

                @Override
                public String gitUsername() {
                    return null;
                }

                @Override
                public String gitPassword() {
                    return null;
                }
            };
        } else {
            throw new AzureExecutionException(String.format(FAILED_TO_PARSE_PUBLISHING_PROFILE, publishingProfileXml));
        }
    }

    public static <T extends WebAppBase> T findAppServiceInPagedList(PagedList<T> list, String resourceGroup, String name) {
        if (StringUtils.isEmpty(resourceGroup) || StringUtils.isEmpty(name)) {
            return null;
        }
        final Iterator<T> iterator = list.listIterator();
        return IteratorUtils.find(iterator, (appBase) -> StringUtils.equals(appBase.resourceGroupName(), resourceGroup) &&
                StringUtils.equals(appBase.name(), name));
    }
}
