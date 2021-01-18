/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.microsoft.rest.ExpandableStringEnum;

import java.util.Collection;

/**
 * Defines values for JavaRuntimeVersion.
 */
public final class SpringCloudRuntimeVersion extends ExpandableStringEnum<SpringCloudRuntimeVersion> {
    /**
     * Static value Java_8 for JavaRuntimeVersion.
     */
    public static final SpringCloudRuntimeVersion JAVA_8 = fromString("Java_8");

    /**
     * Static value Java_11 for JavaRuntimeVersion.
     */
    public static final SpringCloudRuntimeVersion JAVA_11 = fromString("Java_11");

    /**
     * Creates or finds a JavaRuntimeVersion from its string representation.
     *
     * @param name a name to look for
     * @return the corresponding JavaRuntimeVersion
     */
    @JsonCreator
    public static SpringCloudRuntimeVersion fromString(String name) {
        return fromString(name, SpringCloudRuntimeVersion.class);
    }

    /**
     * @return known JavaRuntimeVersion values
     */
    public static Collection<SpringCloudRuntimeVersion> values() {
        return values(SpringCloudRuntimeVersion.class);
    }
}
