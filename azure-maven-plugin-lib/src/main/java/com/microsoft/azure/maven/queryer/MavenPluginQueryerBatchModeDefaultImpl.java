/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.queryer;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

public class MavenPluginQueryerBatchModeDefaultImpl extends MavenPluginQueryer {

    private Log log;

    public MavenPluginQueryerBatchModeDefaultImpl(Log log) {
        this.log = log;
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, List<String> options, String prompt)
        throws MojoFailureException {
        final String initValue = System.getProperty(attribute);
        final String input = StringUtils.isNotEmpty(initValue) ? initValue : defaultValue;
        if (validateInputByOptions(input, options)) {
            log.info(String.format("Use %s for %s", input, attribute));
            return input;
        }
        throw new MojoFailureException(String.format("Invalid input for %s : %s", attribute, input));
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, String regex, String errorMessage,
                                      String prompt) throws MojoFailureException {
        final String initValue = System.getProperty(attribute);
        final String input = StringUtils.isNotEmpty(initValue) ? initValue : defaultValue;
        if (StringUtils.isNotEmpty(input) && validateInputByRegex(input, regex)) {
            log.info(String.format("Use %s for %s", input, attribute));
            return input;
        } else {
            throw new MojoFailureException(String.format("Invalid input for %s : %s", attribute, input));
        }
    }

    @Override
    public void close() {
    }
}
