/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices;

import com.azure.resourcemanager.cognitiveservices.CognitiveServicesManager;
import com.azure.resourcemanager.cognitiveservices.models.ResourceSku;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountSku;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CognitiveServicesSubscription extends AbstractAzServiceSubscription<CognitiveServicesSubscription, CognitiveServicesManager> {
    @Nonnull
    @Getter
    private final String subscriptionId;
    private final CognitiveAccountModule accountModule;

    public CognitiveServicesSubscription(CognitiveServicesManager cognitiveServicesManager, AzureCognitiveServices azureCognitiveServices) {
        super(cognitiveServicesManager.serviceClient().getSubscriptionId(), azureCognitiveServices);
        this.subscriptionId = cognitiveServicesManager.serviceClient().getSubscriptionId();
        this.accountModule = new CognitiveAccountModule(this);
    }

    public CognitiveAccountModule accounts() {
        return this.accountModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(accountModule);
    }
}
