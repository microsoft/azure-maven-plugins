/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core.maven;

import com.azure.core.credential.TokenCredential;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.tools.auth.core.ICredentialRetriever;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.tools.auth.util.AzureEnvironmentUtils;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import rx.Single;

import java.util.Objects;

public class MavenLoginCredentialRetriever implements ICredentialRetriever {
    private static final String INVALID_AZURE_ENVIRONMENT = "Invalid environment string '%s', please replace it with one of " +
            "\"Azure\", \"AzureChina\", \"AzureGermany\", \"AzureUSGovernment\",.";

    @Setter
    protected AzureEnvironment env;

    public MavenLoginCredentialRetriever(AzureEnvironment env) {
        this.env = env;
    }

    @Override
    public Single<AzureCredentialWrapper> retrieve() {
        if (!MavenLoginHelper.existsAzureSecretFile()) {
            return Single.error(new LoginFailureException("Cannot get credentials from maven azure plugin."));
        }

        return Single.fromCallable(() -> {
            final AzureCredential credentials = MavenLoginHelper.readAzureCredentials(MavenLoginHelper.getAzureSecretFile());
            AzureEnvironment envFromMavenLogin = AzureEnvironmentUtils.stringToAzureEnvironment(credentials.getEnvironment());
            if (this.env != null && envFromMavenLogin != null && !Objects.equals(env, envFromMavenLogin)) {
                throw new LoginFailureException(String.format("The azure cloud from maven login '%s' doesn't match with your auth configuration, " +
                                "please switch to other auth method for '%s' environment.",
                        AzureEnvironmentUtils.azureEnvironmentToString(envFromMavenLogin),
                        AzureEnvironmentUtils.getCloudNameForAzureCli(env)));
            }
            if (envFromMavenLogin == null) {
                envFromMavenLogin = AzureEnvironment.AZURE;
            }
            if (StringUtils.isNotBlank(credentials.getEnvironment()) && envFromMavenLogin == null) {
                throw new InvalidConfigurationException(String.format(INVALID_AZURE_ENVIRONMENT, credentials.getEnvironment()));
            }
            final AzureTokenCredentials credentialTrack1 = MavenLoginHelper.getMavenAzureLoginCredentialsTrack1(credentials, envFromMavenLogin);
            final TokenCredential credentialTrack2 = MavenLoginHelper.getMavenAzureLoginCredentialsTrack2(credentials, envFromMavenLogin);
            return new AzureCredentialWrapper(AuthMethod.AZURE_SECRET_FILE, credentialTrack2, envFromMavenLogin) {
                public AzureTokenCredentials getAzureTokenCredentials() {
                    return credentialTrack1;
                }
            };
        });
    }
}
