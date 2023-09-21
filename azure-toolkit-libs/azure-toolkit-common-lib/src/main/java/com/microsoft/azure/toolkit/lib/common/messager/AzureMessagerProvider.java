package com.microsoft.azure.toolkit.lib.common.messager;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface AzureMessagerProvider {
    @Nonnull
    IAzureMessager getMessager();
}
