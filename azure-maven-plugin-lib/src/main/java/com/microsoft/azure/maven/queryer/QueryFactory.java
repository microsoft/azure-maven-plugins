package com.microsoft.azure.maven.queryer;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;

public class QueryFactory {
    public static MavenPluginQueryer getQueryer(Settings settings, Log log) {
        return (settings != null && !settings.isInteractiveMode()) ?
            new MavenPluginQueryerBatchModeDefaultImpl(log) :
            new MavenPluginQueryerDefaultImpl(log);
    }
}
