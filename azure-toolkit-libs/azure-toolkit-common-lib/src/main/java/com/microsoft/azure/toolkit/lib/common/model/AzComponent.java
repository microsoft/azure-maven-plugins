package com.microsoft.azure.toolkit.lib.common.model;

import javax.annotation.Nonnull;

public interface AzComponent {

    @Nonnull
    String getName();

    @Nonnull
    String getId();
}
