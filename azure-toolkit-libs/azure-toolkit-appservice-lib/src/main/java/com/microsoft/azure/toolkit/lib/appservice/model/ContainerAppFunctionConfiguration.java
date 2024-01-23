package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.FunctionApp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class ContainerAppFunctionConfiguration {
    protected Integer minReplicas;
    protected Integer maxReplicas;

    public static ContainerAppFunctionConfiguration fromFunctionApp(@Nonnull final FunctionApp app) {
        return ContainerAppFunctionConfiguration.builder()
            .minReplicas(app.minReplicas())
            .minReplicas(app.maxReplicas()).build();
    }
}
