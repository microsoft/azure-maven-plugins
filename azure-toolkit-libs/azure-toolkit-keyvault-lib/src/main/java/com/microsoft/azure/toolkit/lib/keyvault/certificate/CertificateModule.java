/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.certificate;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource.isHttpException;
import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyConfiureAction;
import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyLearnMoreAction;

public class CertificateModule extends AbstractAzResourceModule<Certificate, KeyVault, CertificateProperties> {
    public static final String NAME = "certificates";

    public CertificateModule(KeyVault parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, CertificateProperties>> loadResourcePagesFromAzure() {
        try {
            return Optional.ofNullable(getClient())
                .map(keys -> keys.listPropertiesOfCertificates().collectList().block())
                .map(ItemPage::new)
                .map(IteratorUtils::singletonIterator)
                .orElseGet(IteratorUtils::emptyIterator);
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                final Action<String> configure = getAccessPolicyConfiureAction(getParent());
                final Action<String> learnMore = getAccessPolicyLearnMoreAction();
                throw new AzureToolkitRuntimeException(ExceptionUtils.getRootCauseMessage(t), configure, learnMore);
            }
            throw t;
        }
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/keyvault.load_certificate.certificate", params = {"name"})
    protected CertificateProperties loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        try {
            final List<CertificateProperties> versions = Optional.ofNullable(getClient())
                .map(keys -> keys.listPropertiesOfCertificates().collectList().block())
                .orElse(null);
            return Objects.isNull(versions) ? null : versions.stream()
                .filter(s -> StringUtils.equalsIgnoreCase(s.getName(), name))
                .findFirst()
                .orElse(null);
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                final Action<String> configure = getAccessPolicyConfiureAction(getParent());
                final Action<String> learnMore = getAccessPolicyLearnMoreAction();
                throw new AzureToolkitRuntimeException(ExceptionUtils.getRootCauseMessage(t), configure, learnMore);
            }
            throw t;
        }
    }

    @Override
    @AzureOperation(name = "azure/keyvault.delete_certificate.certificate", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final CertificateAsyncClient client = getClient();
        if (Objects.isNull(client)) {
            return;
        }
        final ResourceId id = ResourceId.fromString(resourceId);
        client.deleteCertificateOperation(id.name()).block();
    }

    @Nonnull
    @Override
    protected Certificate newResource(@Nonnull CertificateProperties remote) {
        return new Certificate(remote, this);
    }

    @Nonnull
    @Override
    protected Certificate newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Certificate(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected CertificateDraft newDraftForCreate(@Nonnull String name, @org.jetbrains.annotations.Nullable String rgName) {
        return new CertificateDraft(name, Objects.requireNonNull(rgName), this);
    }

    @Nullable
    @Override
    protected CertificateAsyncClient getClient() {
        return getParent().getCertificateClient();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Certificate";
    }
}
