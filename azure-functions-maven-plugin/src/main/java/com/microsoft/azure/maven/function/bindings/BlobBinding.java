/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.BlobTrigger;
import com.microsoft.azure.serverless.functions.annotation.BlobInput;
import com.microsoft.azure.serverless.functions.annotation.BlobOutput;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BlobBinding extends StorageBaseBinding {
    private String path = "";

    public BlobBinding(final BlobTrigger blobTrigger) {
        setDirection("in");
        setType("blobTrigger");
        setName(blobTrigger.name());

        path = blobTrigger.path();
        setConnection(blobTrigger.connection());
    }

    public BlobBinding(final BlobInput blobInput) {
        setDirection("in");
        setType("blob");
        setName(blobInput.name());

        path = blobInput.path();
        setConnection(blobInput.connection());
    }

    public BlobBinding(final BlobOutput blobOutput) {
        setDirection("out");
        setType("blob");
        setName(blobOutput.name());

        path = blobOutput.path();
        setConnection(blobOutput.connection());
    }

    @JsonGetter
    public String getPath() {
        return path;
    }
}
