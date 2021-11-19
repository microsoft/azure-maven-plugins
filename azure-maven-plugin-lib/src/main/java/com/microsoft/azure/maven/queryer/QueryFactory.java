/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.queryer;

import org.apache.maven.settings.Settings;

public class QueryFactory {
    public static MavenPluginQueryer getQueryer(Settings settings) {
        return (settings != null && !settings.isInteractiveMode()) ?
            new MavenPluginQueryerBatchModeDefaultImpl() :
            new TextIOMavenPluginQueryer();
    }
}
