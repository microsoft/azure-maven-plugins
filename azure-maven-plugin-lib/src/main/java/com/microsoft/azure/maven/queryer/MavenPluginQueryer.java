/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.queryer;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class MavenPluginQueryer {
    public abstract String assureInputFromUser(String attribute, String defaultValue,
                                               List<String> options, String prompt) throws MojoFailureException;

    public abstract String assureInputFromUser(String attribute, String defaultValue,
                                               String regex, String prompt, String errorMessage)
        throws MojoFailureException;

    public abstract void close();

    public String assureInputFromUser(String attribute, Enum defaultValue, String prompt) throws MojoFailureException {
        final String defaultValueForAttribute = defaultValue.name();
        final Set<String> optionSet = new HashSet<>();
        for (final Enum option : defaultValue.getClass().getEnumConstants()) {
            optionSet.add(option.name().toLowerCase(Locale.ENGLISH));
        }
        final ArrayList<String> options = new ArrayList<>(optionSet);
        Collections.sort(options);
        return assureInputFromUser(attribute, defaultValueForAttribute, options, prompt);
    }

    protected boolean validateInputByOptions(String input, List<String> options) {
        for (final String option : options) {
            if (option.equalsIgnoreCase(input)) {
                return true;
            }
        }
        return false;
    }

    protected boolean validateInputByRegex(String input, String regex) {
        return StringUtils.isEmpty(regex) || (input != null && input.matches(regex));
    }

    protected String getInitValue(String attribute) {
        return System.getProperty(attribute);
    }

}
