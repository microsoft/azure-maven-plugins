/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.toolkit.lib.common.model.ExpandableParameter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
@EqualsAndHashCode
public class Kind implements ExpandableParameter {

    public static final Kind STORAGE_V2 = new Kind(Performance.STANDARD, "StorageV2", "General Purpose v2");
    public static final Kind BLOB_STORAGE = new Kind(Performance.PREMIUM, "BlobStorage", "Blob Storage");
    public static final Kind BLOCK_BLOB_STORAGE = new Kind(Performance.PREMIUM, "BlockBlobStorage", "Block Blobs Storage");
    public static final Kind FILE_STORAGE = new Kind(Performance.PREMIUM, "FileStorage", "File Storage");
    public static final Kind PAGE_BLOB_STORAGE = new Kind(Performance.PREMIUM, "StorageV2", "Page Blobs Storage");

    private static final List<Kind> values = new ImmutableList.Builder<Kind>().add(STORAGE_V2, BLOCK_BLOB_STORAGE, FILE_STORAGE, PAGE_BLOB_STORAGE).build();

    private Performance performance;
    private String name;
    private String label;

    private Kind(Performance performance, String name, String label) {
        this.performance = performance;
        this.name = name;
        this.label = label;
    }

    public static List<Kind> values() {
        return values;
    }

    public static Kind fromName(@Nonnull String value) {
        return values().stream()
                .filter(region -> StringUtils.equalsAnyIgnoreCase(value, region.name, region.label))
                .findFirst().orElse(new Kind(Performance.STANDARD, value, value));
    }

    @Override
    public boolean isExpandedValue() {
        return !values().contains(this);
    }

}
