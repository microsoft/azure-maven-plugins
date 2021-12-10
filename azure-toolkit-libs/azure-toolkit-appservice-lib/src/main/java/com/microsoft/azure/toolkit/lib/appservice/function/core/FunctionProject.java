/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.function.core;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;

@Setter
@Getter
public class FunctionProject {
    private String name;
    private File stagingFolder;
    private File baseDirectory;
    private File artifactFile;
    private List<File> dependencies;

    private File classesOutputDirectory;
    private File resourceOutputDirectory;

    private File hostJsonFile;
    private File localSettingsJsonFile;
}
