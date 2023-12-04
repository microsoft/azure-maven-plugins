/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.key;

import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.keys.models.CreateEcKeyOptions;
import com.azure.security.keyvault.keys.models.CreateKeyOptions;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.azure.security.keyvault.keys.models.KeyType;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyConfiureAction;
import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyLearnMoreAction;

public class KeyVersionDraft extends KeyVersion
    implements AzResource.Draft<KeyVersion, KeyProperties> {
    public static final String KEY_CREATION_FORBIDDEN_MESSAGE = "failed to create key %s, access denied, please make sure that you have access policy defined to do this operation";
    public static final String KEY_CREATION_FAILED_MESSAGE = "failed to create key %s, an unexpected error occurred";
    public static final String KEY_UPDATE_FORBIDDEN_MESSAGE = "failed to update key %s, access denied, please make sure that you have access policy defined to do this operation";
    public static final String KEY_UPDATE_FAILED_MESSAGE = "failed to update key %s, an unexpected error occurred";

    @Getter
    private final KeyVersion origin;

    @Setter
    private KeyDraft.Config config;

    protected KeyVersionDraft(@Nonnull KeyVersion origin) {
        super(origin);
        this.origin = origin;
    }


    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.create_key_version.version", params = {"this.getName()"})
    public KeyProperties createResourceInAzure() {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.update_key_version.version", params = {"this.getName()"})
    public KeyProperties updateResourceInAzure(@Nonnull KeyProperties origin) {
        final KeyDraft.Config config = ensureConfig();
        final Boolean isEnabled = config.getEnabled();
        final boolean isModified = Objects.nonNull(isEnabled) && !Objects.equals(isEnabled, origin.isEnabled());
        if (isModified) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating Key Version ({0}).", this.getVersion()));
            final KeyProperties keyProperties = updateKeyVersion(getKeyVault(), origin, config);
            messager.info(AzureString.format("Key Version ({0}) is successfully updated.", this.getVersion()));
            return keyProperties;
        }
        return origin;
    }

    public static KeyProperties createKeyVersion(@Nonnull final KeyVault keyVault, @Nonnull final KeyDraft.Config config) {
        final KeyAsyncClient keyClient = keyVault.getKeyClient();
        try {
            final KeyType keyType = Objects.requireNonNull(config.getKeyType(), "Type is required to create a key");
            final CreateKeyOptions options;
            if (keyType == KeyType.RSA) {
                options = new CreateRsaKeyOptions(config.getName())
                    .setKeySize(config.getRasKeySize());
            } else if (keyType == KeyType.EC) {
                options = new CreateEcKeyOptions(config.getName())
                    .setCurveName(config.getCurveName());
            } else {
                throw new AzureToolkitRuntimeException("Not support key type: " + keyType);
            }
            options.setEnabled(config.getEnabled());
            options.setNotBefore(config.getActivationDate());
            options.setExpiresOn(config.getExpirationDate());
            return Objects.requireNonNull(keyClient.createKey(options).block(), "failed to create key").getProperties();
        } catch (final Throwable t) {
            // swallow all exceptions to prevent credential leakage
            final Action<String> configure = getAccessPolicyConfiureAction(keyVault);
            final Action<String> learnMore = getAccessPolicyLearnMoreAction();
            if (isHttpException(t, 403)) {
                throw new AzureToolkitRuntimeException(String.format(KEY_CREATION_FORBIDDEN_MESSAGE, config.getName()), configure, learnMore);
            }
            throw new AzureToolkitRuntimeException(String.format(KEY_CREATION_FAILED_MESSAGE, config.getName()));
        }
    }

    public static KeyProperties updateKeyVersion(@Nonnull final KeyVault keyVault,
                                                 @Nonnull final KeyProperties origin,
                                                 @Nonnull final KeyDraft.Config config) {
        final KeyAsyncClient client = keyVault.getKeyClient();
        try {
            origin.setEnabled(config.getEnabled());
            // workaround to fix issue that exportable is also included in request, which is only support with API 7.3-preview
            origin.setExportable(null);
            return Objects.requireNonNull(client.updateKeyProperties(origin).block(), "failed to update secret").getProperties();
        } catch (final Throwable t) {
            // swallow all exceptions to prevent credential leakage
            final Action<String> configure = getAccessPolicyConfiureAction(keyVault);
            final Action<String> learnMore = getAccessPolicyLearnMoreAction();
            if (isHttpException(t, 403)) {
                throw new AzureToolkitRuntimeException(String.format(KEY_UPDATE_FORBIDDEN_MESSAGE, config.getName()), configure, learnMore);
            }
            throw new AzureToolkitRuntimeException(String.format(KEY_UPDATE_FAILED_MESSAGE, config.getName()));
        }
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    private KeyDraft.Config ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(KeyDraft.Config::new);
        return this.config;
    }

    public void setEnabled(boolean b) {
        ensureConfig().setEnabled(b);
    }
}

