/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class DiagnosticConfig {
    @Builder.Default
    boolean enableWebServerLogging = true;
    @Builder.Default
    Integer webServerLogQuota = 35;
    @Builder.Default
    Integer webServerRetentionPeriod = 0;
    @Builder.Default
    boolean enableDetailedErrorMessage = false;
    @Builder.Default
    boolean enableFailedRequestTracing = false;
    // application log
    @Builder.Default
    boolean enableApplicationLog = true;
    @Builder.Default
    LogLevel applicationLogLevel = LogLevel.ERROR;
}
