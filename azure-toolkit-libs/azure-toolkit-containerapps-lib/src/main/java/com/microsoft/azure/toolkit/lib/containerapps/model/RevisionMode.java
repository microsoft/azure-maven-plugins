/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.model;

import com.azure.resourcemanager.appcontainers.models.ActiveRevisionsMode;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class RevisionMode {
    public static final RevisionMode MULTIPLE = new RevisionMode("Multiple");
    public static final RevisionMode SINGLE = new RevisionMode("Single");

    private String value;

    public static List<RevisionMode> values() {
        return Arrays.asList(MULTIPLE, SINGLE);
    }

    @Nullable
    public static RevisionMode fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElse(null);
    }

    public ActiveRevisionsMode toActiveRevisionMode() {
        return ActiveRevisionsMode.fromString(this.value);
    }
}
