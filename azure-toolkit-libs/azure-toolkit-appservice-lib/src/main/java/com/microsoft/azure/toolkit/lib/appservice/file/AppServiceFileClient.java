/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.file;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import rx.Observable;

import java.util.List;

public interface AppServiceFileClient {
    Observable<ResponseBody> getFileContent(String path);

    Observable<? extends List<? extends AppServiceFile>> getFilesInDirectory(String path);

    Observable<Void> saveFile(String path, RequestBody requestBody);

    Observable<ResponseBody> createDirectory(String path);

    Observable<ResponseBody> deleteFile(String path);
}
