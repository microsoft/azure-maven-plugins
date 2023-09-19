package com.microsoft.azure.toolkit.lib.common.messager;

@FunctionalInterface
public interface AzureMessagerProvider {
    IAzureMessager getMessager();
}
