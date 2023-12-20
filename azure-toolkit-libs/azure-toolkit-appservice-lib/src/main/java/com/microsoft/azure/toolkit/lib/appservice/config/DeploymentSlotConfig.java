package com.microsoft.azure.toolkit.lib.appservice.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentSlotConfig {
    private String name;
    private String configurationSource;
}
