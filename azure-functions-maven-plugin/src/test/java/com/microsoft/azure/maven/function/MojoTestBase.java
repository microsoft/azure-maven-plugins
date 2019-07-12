/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;

import java.io.File;

public class MojoTestBase {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    protected Mojo getMojoFromPom(final String filename, final String goal) throws Exception {
        final File pom = new File(this.getClass().getResource(filename).toURI());
        return rule.lookupMojo(goal, pom);
    }
}
