/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkit.lib.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public class Region {
    public static final Region US_EAST = new Region("eastus", "East US");
    public static final Region US_EAST2 = new Region("eastus2", "East US 2");
    public static final Region US_SOUTH_CENTRAL = new Region("southcentralus", "South Central US");
    public static final Region US_WEST2 = new Region("westus2", "West US 2");
    public static final Region US_CENTRAL = new Region("centralus", "Central US");
    public static final Region US_NORTH_CENTRAL = new Region("northcentralus", "North Central US");
    public static final Region US_WEST = new Region("westus", "West US");
    public static final Region US_WEST_CENTRAL = new Region("westcentralus", "West Central US");
    public static final Region CANADA_CENTRAL = new Region("canadacentral", "Canada Central");
    public static final Region CANADA_EAST = new Region("canadaeast", "Canada East");
    public static final Region BRAZIL_SOUTH = new Region("brazilsouth", "Brazil South");
    public static final Region BRAZIL_SOUTHEAST = new Region("brazilsoutheast", "Brazil Southeast");
    public static final Region EUROPE_NORTH = new Region("northeurope", "North Europe");
    public static final Region UK_SOUTH = new Region("uksouth", "UK South");
    public static final Region EUROPE_WEST = new Region("westeurope", "West Europe");
    public static final Region FRANCE_CENTRAL = new Region("francecentral", "France Central");
    public static final Region GERMANY_WEST_CENTRAL = new Region("germanywestcentral", "Germany West Central");
    public static final Region NORWAY_EAST = new Region("norwayeast", "Norway East");
    public static final Region SWITZERLAND_NORTH = new Region("switzerlandnorth", "Switzerland North");
    public static final Region FRANCE_SOUTH = new Region("francesouth", "France South");
    public static final Region GERMANY_NORTH = new Region("germanynorth", "Germany North");
    public static final Region NORWAY_WEST = new Region("norwaywest", "Norway West");
    public static final Region SWITZERLAND_WEST = new Region("switzerlandwest", "Switzerland West");
    public static final Region UK_WEST = new Region("ukwest", "UK West");
    public static final Region AUSTRALIA_EAST = new Region("australiaeast", "Australia East");
    public static final Region ASIA_SOUTHEAST = new Region("southeastasia", "Southeast Asia");
    public static final Region INDIA_CENTRAL = new Region("centralindia", "Central India");
    public static final Region ASIA_EAST = new Region("eastasia", "East Asia");
    public static final Region JAPAN_EAST = new Region("japaneast", "Japan East");
    public static final Region KOREA_CENTRAL = new Region("koreacentral", "Korea Central");
    public static final Region AUSTRALIA_CENTRAL = new Region("australiacentral", "Australia Central");
    public static final Region AUSTRALIA_CENTRAL2 = new Region("australiacentral2", "Australia Central 2");
    public static final Region AUSTRALIA_SOUTHEAST = new Region("australiasoutheast", "Australia Southeast");
    public static final Region JAPAN_WEST = new Region("japanwest", "Japan West");
    public static final Region KOREA_SOUTH = new Region("koreasouth", "Korea South");
    public static final Region INDIA_SOUTH = new Region("southindia", "South India");
    public static final Region INDIA_WEST = new Region("westindia", "West India");
    public static final Region UAE_NORTH = new Region("uaenorth", "UAE North");
    public static final Region UAE_CENTRAL = new Region("uaecentral", "UAE Central");
    public static final Region SOUTHAFRICA_NORTH = new Region("southafricanorth", "South Africa North");
    public static final Region SOUTHAFRICA_WEST = new Region("southafricawest", "South Africa West");
    public static final Region CHINA_NORTH = new Region("chinanorth", "China North");
    public static final Region CHINA_EAST = new Region("chinaeast", "China East");
    public static final Region CHINA_NORTH2 = new Region("chinanorth2", "China North 2");
    public static final Region CHINA_EAST2 = new Region("chinaeast2", "China East 2");
    public static final Region GERMANY_CENTRAL = new Region("germanycentral", "Germany Central");
    public static final Region GERMANY_NORTHEAST = new Region("germanynortheast", "Germany Northeast");
    public static final Region GOV_US_VIRGINIA = new Region("usgovvirginia", "US Gov Virginia");
    public static final Region GOV_US_IOWA = new Region("usgoviowa", "US Gov Iowa");
    public static final Region GOV_US_ARIZONA = new Region("usgovarizona", "US Gov Arizona");
    public static final Region GOV_US_TEXAS = new Region("usgovtexas", "US Gov Texas");
    public static final Region GOV_US_DOD_EAST = new Region("usdodeast", "US DoD East");
    public static final Region GOV_US_DOD_CENTRAL = new Region("usdodcentral", "US DoD Central");
    private final String name;
    private final String label;

    public static List<Region> values() {
        return Arrays.asList(US_EAST, US_EAST2, US_SOUTH_CENTRAL, US_WEST2, US_CENTRAL, US_NORTH_CENTRAL, US_WEST, US_WEST_CENTRAL, CANADA_CENTRAL,
            CANADA_EAST, BRAZIL_SOUTH, BRAZIL_SOUTHEAST, EUROPE_NORTH, UK_SOUTH, EUROPE_WEST, FRANCE_CENTRAL, GERMANY_WEST_CENTRAL, NORWAY_EAST,
            SWITZERLAND_NORTH, FRANCE_SOUTH, GERMANY_NORTH, NORWAY_WEST, SWITZERLAND_WEST, UK_WEST, AUSTRALIA_EAST, ASIA_SOUTHEAST, INDIA_CENTRAL,
            ASIA_EAST, JAPAN_EAST, KOREA_CENTRAL, AUSTRALIA_CENTRAL, AUSTRALIA_CENTRAL2, AUSTRALIA_SOUTHEAST, JAPAN_WEST, KOREA_SOUTH, INDIA_SOUTH,
            INDIA_WEST, UAE_NORTH, UAE_CENTRAL, SOUTHAFRICA_NORTH, SOUTHAFRICA_WEST, CHINA_NORTH, CHINA_EAST, CHINA_NORTH2, CHINA_EAST2, GERMANY_CENTRAL,
            GERMANY_NORTHEAST, GOV_US_VIRGINIA, GOV_US_IOWA, GOV_US_ARIZONA, GOV_US_TEXAS, GOV_US_DOD_EAST, GOV_US_DOD_CENTRAL);
    }

    public static Region fromName(String value) {
        return values().stream()
            .filter(region -> StringUtils.equalsAnyIgnoreCase(value, region.name, region.label))
            .findFirst().orElse(null);
    }
}
