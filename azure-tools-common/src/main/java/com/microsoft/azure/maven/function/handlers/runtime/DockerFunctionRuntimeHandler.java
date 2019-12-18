/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.runtime;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.appservice.DockerImageType;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import static com.microsoft.azure.maven.function.Constants.*;

public class DockerFunctionRuntimeHandler extends AbstractLinuxFunctionRuntimeHandler {

    private static final String INVALID_DOCKER_RUNTIME = "Invalid docker runtime configured.";

    public static class Builder extends FunctionRuntimeHandler.Builder<DockerFunctionRuntimeHandler.Builder> {

        @Override
        public DockerFunctionRuntimeHandler build() {
            return new DockerFunctionRuntimeHandler(self());
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    protected DockerFunctionRuntimeHandler(Builder builder) {
        super(builder);
    }

    @Override
    public FunctionApp.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException {
        final DockerImageType imageType = AppServiceUtils.getDockerImageType(image, dockerCredentialProvider != null,
                registryUrl);
        checkFunctionExtensionVersion();
        checkServerConfiguration(imageType, dockerCredentialProvider);

        final FunctionApp.DefinitionStages.WithDockerContainerImage withDockerContainerImage = super.defineLinuxFunction();
        final FunctionApp.DefinitionStages.WithCreate result;
        switch (imageType) {
            case PUBLIC_DOCKER_HUB:
                result = withDockerContainerImage.withPublicDockerHubImage(image);
                break;
            case PRIVATE_DOCKER_HUB:
                result = withDockerContainerImage.withPrivateDockerHubImage(image).withCredentials(dockerCredentialProvider.getUsername(), dockerCredentialProvider.getPassword());
                break;
            case PRIVATE_REGISTRY:
                result = withDockerContainerImage.withPrivateRegistryImage(image, registryUrl).withCredentials(dockerCredentialProvider.getUsername(), dockerCredentialProvider.getPassword());
                break;
            default:
                throw new AzureExecutionException(INVALID_DOCKER_RUNTIME);
        }
        final String decryptionKey = generateDecryptionKey();
        return (FunctionApp.DefinitionStages.WithCreate) result
                .withAppSetting(APP_SETTING_MACHINEKEY_DECRYPTION_KEY, decryptionKey)
                .withAppSetting(APP_SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, APP_SETTING_DISABLE_WEBSITES_APP_SERVICE_STORAGE)
                .withAppSetting(APP_SETTING_FUNCTION_APP_EDIT_MODE, APP_SETTING_FUNCTION_APP_EDIT_MODE_READONLY);
    }

    @Override
    public FunctionApp.Update updateAppRuntime(FunctionApp app) throws AzureExecutionException {
        final DockerImageType imageType = AppServiceUtils.getDockerImageType(image, dockerCredentialProvider != null,
                registryUrl);
        checkFunctionExtensionVersion();
        checkServerConfiguration(imageType, dockerCredentialProvider);

        final FunctionApp.Update update = app.update();
        switch (imageType) {
            case PUBLIC_DOCKER_HUB:
                return update.withPublicDockerHubImage(image);
            case PRIVATE_DOCKER_HUB:
                return update.withPrivateDockerHubImage(image).withCredentials(dockerCredentialProvider.getUsername(), dockerCredentialProvider.getPassword());
            case PRIVATE_REGISTRY:
                return update.withPrivateRegistryImage(image, registryUrl).withCredentials(dockerCredentialProvider.getUsername(), dockerCredentialProvider.getPassword());
            default:
                throw new AzureExecutionException(INVALID_DOCKER_RUNTIME);
        }
    }

    private void checkServerConfiguration(DockerImageType imageType, IDockerCredentialProvider dockerCredentialProvider)
            throws AzureExecutionException {
        if (imageType != DockerImageType.PUBLIC_DOCKER_HUB) {
            if (dockerCredentialProvider == null) {
                throw new AzureExecutionException("Cannot get docker credential for private image.");
            }
            if (StringUtils.isEmpty(dockerCredentialProvider.getUsername())) {
                throw new AzureExecutionException("Cannot get username for private image.");
            }
            if (StringUtils.isEmpty(dockerCredentialProvider.getPassword())) {
                throw new AzureExecutionException("Cannot get password for private image.");
            }
        }
    }

    protected String generateDecryptionKey() {
        // Refers https://github.com/Azure/azure-cli/blob/dev/src/azure-cli/azure/cli/command_modules/appservice/custom.py#L2300
        return Hex.encodeHexString(RandomUtils.nextBytes(32)).toUpperCase();
    }
}
