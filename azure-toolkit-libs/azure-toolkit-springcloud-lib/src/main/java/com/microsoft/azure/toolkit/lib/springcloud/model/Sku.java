package com.microsoft.azure.toolkit.lib.springcloud.model;

import com.azure.resourcemanager.appplatform.models.SkuName;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Sku {
    @Nonnull
    @EqualsAndHashCode.Include
    private final String name;
    @Nonnull
    @EqualsAndHashCode.Include
    private final String tier;

    public Sku(com.azure.resourcemanager.appplatform.models.Sku sku) {
        super();
        this.name = sku.name();
        this.tier = sku.tier();
    }

    public String getLabel() {
        if (tier.equalsIgnoreCase("StandardGen2")) {
            return "Standard Consumption & Dedicated (preview)";
        } else if (name.equalsIgnoreCase(SkuName.B0.toString())) {
            return "Basic";
        } else if (name.equalsIgnoreCase(SkuName.S0.toString())) {
            return "Standard";
        } else if (name.equalsIgnoreCase(SkuName.E0.toString())) {
            return "Enterprise";
        }
        throw new AzureToolkitRuntimeException("invalid sku");
    }

    public int getOrdinal() {
        if (tier.equalsIgnoreCase("StandardGen2")) {
            return 0;
        } else if (name.equalsIgnoreCase(SkuName.B0.toString())) {
            return 1;
        } else if (name.equalsIgnoreCase(SkuName.S0.toString())) {
            return 2;
        } else if (name.equalsIgnoreCase(SkuName.E0.toString())) {
            return 3;
        }
        throw new AzureToolkitRuntimeException("invalid sku");
    }

    public String getDescription() {
        if (tier.equalsIgnoreCase("StandardGen2")) {
            return "0 base price + actual usage, 400/1000 app instances max.";
        } else if (name.equalsIgnoreCase(SkuName.B0.toString())) {
            return "2 vCPUs, 4 Gi included, 25 app instances max.";
        } else if (name.equalsIgnoreCase(SkuName.S0.toString())) {
            return "6 vCPUs, 12 Gi included, 500 app instances max.";
        } else if (name.equalsIgnoreCase(SkuName.E0.toString())) {
            return "6 vCPUs, 12 Gi included, 1000 app instances max.";
        }
        throw new AzureToolkitRuntimeException("invalid sku");
    }

    public boolean isEnterpriseTier() {
        return name.equalsIgnoreCase(SkuName.E0.toString());
    }

    public com.azure.resourcemanager.appplatform.models.Sku toSku() {
        return new com.azure.resourcemanager.appplatform.models.Sku().withName(name).withTier(tier);
    }

    public static Sku fromSku(com.azure.resourcemanager.appplatform.models.Sku sku) {
        return new Sku(sku);
    }
}
