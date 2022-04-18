/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.action;

import com.microsoft.azure.toolkit.lib.common.view.IView;

import java.util.List;

public interface IActionGroup {

    IView.Label getView();

    List<Object> getActions();

    void addAction(Object action);
}
