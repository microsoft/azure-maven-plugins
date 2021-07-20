package com.microsoft.azure.toolkit.lib.common.bundle;


import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;

public interface CustomDecoratable {
    /**
     * @return null if not decoratable
     */
    String decorate(IAzureMessage message);
}
