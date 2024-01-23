/*
  Copyright (c) Microsoft Corporation. All rights reserved.
  Licensed under the MIT License. See License.txt in the project root for
  license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import java.beans.Transient;

public interface ExpandableParameter {
    @Transient
    boolean isExpandedValue();
}
