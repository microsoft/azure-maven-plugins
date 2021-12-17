/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.microsoft.azure.toolkit.lib.common.action.Action;

public interface IAccountActions {
    Action.Id<Void> TRY_AZURE = Action.Id.of("action.account.try_azure");
    Action.Id<Void> SELECT_SUBS = Action.Id.of("action.account.select_subs");
    Action.Id<Void> AUTHENTICATE = Action.AUTHENTICATE;
}
