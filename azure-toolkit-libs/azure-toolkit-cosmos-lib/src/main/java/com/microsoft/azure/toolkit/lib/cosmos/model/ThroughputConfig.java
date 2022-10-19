/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import com.azure.resourcemanager.cosmos.models.AutoscaleSettings;
import com.azure.resourcemanager.cosmos.models.CreateUpdateOptions;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

@Data
public class ThroughputConfig {
    private Integer throughput;
    private Integer maxThroughput;

    public CreateUpdateOptions toCreateUpdateOptions() {
        assert ObjectUtils.anyNull(throughput, maxThroughput);
        if (ObjectUtils.allNull(throughput, maxThroughput)) {
            return null;
        }
        final CreateUpdateOptions options = new CreateUpdateOptions();
        return Objects.nonNull(throughput) ? options.withThroughput(throughput) :
                options.withAutoscaleSettings(new AutoscaleSettings().withMaxThroughput(maxThroughput));
    }
}
