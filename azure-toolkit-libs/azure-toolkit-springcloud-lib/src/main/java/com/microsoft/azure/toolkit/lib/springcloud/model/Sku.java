package com.microsoft.azure.toolkit.lib.springcloud.model;

import com.azure.resourcemanager.appplatform.models.SkuName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Sku {
    public static final Sku BASIC = new Sku(SkuName.B0.toString(), "Basic");
    public static final Sku STANDARD = new Sku(SkuName.S0.toString(), "Standard");
    public static final Sku ENTERPRISE = new Sku(SkuName.E0.toString(), "Enterprise");
    public static final Sku CONSUMPTION = new Sku(SkuName.S0.toString(), "StandardGen2");
    public static final List<Sku> KNOWN_SKUS = Collections.unmodifiableList(Arrays.asList(BASIC, STANDARD, ENTERPRISE, CONSUMPTION));
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
        if ("StandardGen2".equalsIgnoreCase(tier)) {
            return "Standard Consumption & Dedicated (preview)";
        } else if (name.equalsIgnoreCase(SkuName.B0.toString())) {
            return "Basic";
        } else if (name.equalsIgnoreCase(SkuName.S0.toString())) {
            return "Standard";
        } else if (name.equalsIgnoreCase(SkuName.E0.toString())) {
            return "Enterprise";
        }
        return tier + " " + name;
    }

    public int getOrdinal() {
        if ("StandardGen2".equalsIgnoreCase(tier)) {
            return 0;
        } else if (name.equalsIgnoreCase(SkuName.B0.toString())) {
            return 1;
        } else if (name.equalsIgnoreCase(SkuName.S0.toString())) {
            return 2;
        } else if (name.equalsIgnoreCase(SkuName.E0.toString())) {
            return 3;
        }
        return 4;
    }

    public String getDescription() {
        if ("StandardGen2".equalsIgnoreCase(tier)) {
            return "0 base price + actual usage, 400/1000 app instances max.";
        } else if (name.equalsIgnoreCase(SkuName.B0.toString())) {
            return "2 vCPUs, 4 Gi included, 25 app instances max.";
        } else if (name.equalsIgnoreCase(SkuName.S0.toString())) {
            return "6 vCPUs, 12 Gi included, 500 app instances max.";
        } else if (name.equalsIgnoreCase(SkuName.E0.toString())) {
            return "6 vCPUs, 12 Gi included, 1000 app instances max.";
        }
        return tier + " " + name;
    }

    public boolean isEnterpriseTier() {
        return name.equalsIgnoreCase(SkuName.E0.toString());
    }

    public boolean isBasicTier() {
        return name.equalsIgnoreCase(SkuName.B0.toString());
    }

    public boolean isStandardTier() {
        return name.equalsIgnoreCase(SkuName.S0.toString());
    }

    public boolean isConsumptionTier() {
        return "StandardGen2".equalsIgnoreCase(this.tier);
    }

    public com.azure.resourcemanager.appplatform.models.Sku toSku() {
        return new com.azure.resourcemanager.appplatform.models.Sku().withName(name).withTier(tier);
    }

    @Nullable
    public static Sku fromString(final String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
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
