/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.CommandOutput;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.model.TunnelStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;

public interface IKuduClient {
    Flux<ByteBuffer> getFileContent(final String path);

    List<? extends AppServiceFile> getFilesInDirectory(String dir);

    AppServiceFile getFileByPath(String path);

    void uploadFileToPath(String content, String path);

    void createDirectory(String path);

    void deleteFile(String path);

    List<ProcessInfo> listProcess();

    CommandOutput execute(final String command, final String dir);

    TunnelStatus getAppServiceTunnelStatus();
}
