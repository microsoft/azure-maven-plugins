/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlFirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;

import java.util.List;

public interface ISqlServer {

    SqlServerEntity entity();

    void delete();

    ISqlServerCreator<? extends ISqlServer> create();

    ISqlServerUpdater<? extends ISqlServer> update();

    List<SqlFirewallRuleEntity> firewallRules();

    List<SqlDatabaseEntity> databases();

}
