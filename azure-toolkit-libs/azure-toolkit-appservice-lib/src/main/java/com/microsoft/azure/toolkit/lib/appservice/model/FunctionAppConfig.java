package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionAppConfig {
    public static final FunctionAppConfig DEFAULT_CONFIG = FunctionAppConfig.builder()
        .deployment(FunctionsDeployment.DEFAULT_DEPLOYMENT)
        .runtime(FunctionsRuntime.DEFAULT_RUNTIME)
        .scaleAndConcurrency(FunctionScaleAndConcurrency.DEFAULT_SCALE_CONFIGURATION)
        .build();
    @Nonnull
    private FunctionsDeployment deployment;
    @Nonnull
    private FunctionsRuntime runtime;
    @Nonnull
    private FunctionScaleAndConcurrency scaleAndConcurrency;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionsDeployment {
        public static final FunctionsDeployment DEFAULT_DEPLOYMENT = FunctionsDeployment.builder().storage(Storage.DEFAULT_STORAGE).build();
        @Nonnull
        private Storage storage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Storage {
        public static final Storage DEFAULT_STORAGE = Storage.builder().authentication(Authentication.DEFAULT_AUTHENTICATION).build();
        // Property to select Azure Storage type. Available options: blobContainer.
        @Builder.Default
        private Type type = Type.blobContainer;
        // Property to set the URL for the selected Azure Storage type.
        // Example: For blobContainer, the value could be https://<storageAccountName>.blob.core.windows.net/<containerName>
        private String value;
        private Authentication authentication;

        // Authentication method to access the storage account for deployment
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Authentication {
            public static final String DEPLOYMENT_STORAGE_CONNECTION_STRING = "DEPLOYMENT_STORAGE_CONNECTION_STRING";
            public static final Authentication DEFAULT_AUTHENTICATION = Authentication.builder()
                .type(StorageAuthenticationMethod.StorageAccountConnectionString)
                .storageAccountConnectionStringName(DEPLOYMENT_STORAGE_CONNECTION_STRING)
                .build();
            // Property to select authentication type to access the selected storage account.
            // Available options: SystemAssignedIdentity, UserAssignedIdentity, StorageAccountConnectionString.
            @Builder.Default
            private StorageAuthenticationMethod type = StorageAuthenticationMethod.StorageAccountConnectionString;
            // Use this property for UserAssignedIdentity. Set the resource ID of the identity.
            // Do not set a value for this property when using other authentication type.
            private String userAssignedIdentityResourceId;
            // Use this property for StorageAccountConnectionString. Set the name of the app setting that has the storage account connection string.
            // Do not set a value for this property when using other authentication type.
            @Builder.Default
            private String storageAccountConnectionStringName = DEPLOYMENT_STORAGE_CONNECTION_STRING;
        }

        public enum Type {
            blobContainer
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionsRuntime {
        public static final FunctionsRuntime DEFAULT_RUNTIME = FunctionsRuntime.builder().name("java").version("17").build();
        // Function app runtime name. Available options: dotnet-isolated, node, java, powershell, python, custom
        // hard code java here as we are java tooling
        @Builder.Default
        private String name = "java";
        private String version;
    }

    // Scale and concurrency settings for the function app.
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionScaleAndConcurrency {
        public static final int DEFAULT_INSTANCE_SIZE = 2048;
        public static final int DEFAULT_MAXIMUM_INSTANCE_COUNT = 100;
        public static final FunctionScaleAndConcurrency DEFAULT_SCALE_CONFIGURATION = FunctionScaleAndConcurrency.builder()
            .instanceMemoryMB(DEFAULT_INSTANCE_SIZE)
            .maximumInstanceCount(DEFAULT_MAXIMUM_INSTANCE_COUNT)
            .build();
        // 'Always Ready' configuration for the function app.
        private FunctionAlwaysReadyConfig[] alwaysReady;
        // The maximum number of instances for the function app.
        private Integer maximumInstanceCount;
        // Set the amount of memory allocated to each instance of the function app in MB.
        // CPU and network bandwidth are allocated proportionally.
        private Integer instanceMemoryMB;
        // Scale and concurrency settings for the function app triggers.
        private FunctionTriggers triggers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionTriggers {
        // Scale and concurrency settings for the HTTP trigger.
        private FunctionHttpTriggers http;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionHttpTriggers {
        private Integer perInstanceConcurrency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionAlwaysReadyConfig {
        // Either a function group or a function name is required.
        // For additional information see https://aka.ms/flexconsumption/alwaysready.
        private String name;
        // Sets the number of 'Always Ready' instances for a given function group or a specific function.
        // For additional information see https://aka.ms/flexconsumption/alwaysready.
        private Integer instanceCount;
    }
}
