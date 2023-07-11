package com.microsoft.azure.toolkit.lib.springcloud.model;

import com.azure.resourcemanager.appplatform.models.SkuName;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Sku {
    public static final Sku BASIC = new Sku(SkuName.B0.toString(), "Basic");
    public static final Sku STANDARD = new Sku(SkuName.S0.toString(), "Standard");
    public static final Sku PREMIUM = new Sku(SkuName.E0.toString(), "Premium");
    public static final Sku CONSUMPTION = new Sku(SkuName.S0.toString(), "StandardGen2");
    public static final Sku UNKNOWN = new Sku("Unknown", "Unknown");
    public static final List<Sku> KNOWN_SKUS = Collections.unmodifiableList(Arrays.asList(BASIC, STANDARD, PREMIUM, CONSUMPTION));
    public static final String CONSUMPTION_DISPLAY_NAME = "Consumption";

    public static final Sku SPRING_APPS_DEFAULT_SKU = CONSUMPTION;

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
        return this.tier;
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
        return this.tier + " " + this.name;
    }

    public boolean isEnterpriseTier() {
        return name.equalsIgnoreCase(SkuName.E0.toString());
    }

    public boolean isConsumptionTier() {
        return tier.equalsIgnoreCase("StandardGen2");
    }

    public boolean isStandardTier() {
        return Objects.equals(this, STANDARD);
    }

    public boolean isBasicTier() {
        return Objects.equals(this, BASIC);
    }

    public com.azure.resourcemanager.appplatform.models.Sku toSku() {
        return new com.azure.resourcemanager.appplatform.models.Sku().withName(name).withTier(tier);
    }

    public static Sku fromString(final String value) {
        if (StringUtils.equalsIgnoreCase(value, CONSUMPTION_DISPLAY_NAME)) {
            return CONSUMPTION;
        }
        return KNOWN_SKUS.stream()
            .filter(sku -> StringUtils.equalsIgnoreCase(value, sku.toString()))
            .findFirst()
            .orElseGet(() -> {
                // get sku by pattern: tier/name
                final String[] split = value.split("/");
                return ArrayUtils.getLength(split) > 1 ? new Sku(split[1], split[0]) : new Sku(value, value);
            });
    }

    @Override
    public String toString() {
        if (this.equals(CONSUMPTION)) {
            return CONSUMPTION_DISPLAY_NAME;
        }
        return KNOWN_SKUS.stream()
            .filter(sku -> sku.equals(this))
            .findFirst()
            .map(Sku::getTier)
            .orElseGet(() -> this.tier + "/" + this.name);
    }

    public static Sku fromSku(com.azure.resourcemanager.appplatform.models.Sku sku) {
        return new Sku(sku);
    }
}
