/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.key;

import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class KeyVersion extends AbstractAzResource<KeyVersion, Key, KeyProperties> {

    protected KeyVersion(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull KeyVersionModule module) {
        super(name, resourceGroupName, module);
    }

    protected KeyVersion(@Nonnull KeyVersion origin) {
        super(origin);
    }

    protected KeyVersion(@Nonnull KeyProperties remote, @Nonnull KeyVersionModule module) {
        super(remote.getVersion(), module.getParent().getResourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull KeyProperties remote) {
        return remote.isEnabled() ? FormalStatus.RUNNING.name() : FormalStatus.STOPPED.name();
    }

    public Boolean isEnabled() {
        return Optional.ofNullable(getRemote()).map(KeyProperties::isEnabled).orElse(false);
    }

    public String getVersion() {
        return Optional.ofNullable(getRemote()).map(KeyProperties::getVersion).orElse(null);
    }

    public boolean isCurrentVersion() {
        return StringUtils.equals(getVersion(), getParent().getCurrentVersionId());
    }
}


