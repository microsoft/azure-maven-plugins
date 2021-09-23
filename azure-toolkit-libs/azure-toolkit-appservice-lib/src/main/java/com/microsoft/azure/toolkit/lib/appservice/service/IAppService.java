/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.entity.AppServiceBaseEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.Map;

public interface IAppService<T extends AppServiceBaseEntity> extends IFileClient, IProcessClient, IAzureResource<T> {
    void start();

    void stop();

    void restart();

    void delete();

    boolean exists();

    String hostName();

    String state();

    Runtime getRuntime();

    Region region();

    Map<String, String> appSettings(boolean... force);

    PublishingProfile getPublishingProfile();

    InputStream listPublishingProfileXmlWithSecrets();

    DiagnosticConfig getDiagnosticConfig();

    Flux<String> streamAllLogsAsync();

    IAppServicePlan plan();
}
