/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.microsoft.azure.toolkit.lib.common.action.Action;

public interface IAccountActions {
    Action.Id<Object> TRY_AZURE = Action.TRY_AZURE;
    Action.Id<Object> SELECT_SUBS = Action.SELECT_SUBS;
    Action.Id<Object> AUTHENTICATE = Action.AUTHENTICATE;
}
