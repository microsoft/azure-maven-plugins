/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.model.CommandOutput;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.model.TunnelStatus;

import java.util.List;

public interface IProcessClient {
    List<ProcessInfo> listProcess();

    CommandOutput execute(final String command, final String dir);

    TunnelStatus getAppServiceTunnelStatus();
}
