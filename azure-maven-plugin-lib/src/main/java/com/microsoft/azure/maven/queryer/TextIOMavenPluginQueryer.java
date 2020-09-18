/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.queryer;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.beryx.textio.TextIO;

import com.microsoft.azure.common.logging.Log;

public class TextIOMavenPluginQueryer extends MavenPluginQueryer {
    private static final String FOUND_VALID_VALUE = "Found valid value. Skip user input.";
    private static final String PROMPT_STRING_WITHOUT_DEFAULTVALUE = "Define value for %s";
    private TextIO textIO;

    public TextIOMavenPluginQueryer(TextIO textIO) {
        this.textIO = textIO;
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, List<String> options, String prompt) {
        final String initValue = getInitValue(attribute);
        if (initValue != null && validateInputByOptions(initValue, options)) {
            Log.info(FOUND_VALID_VALUE);
            return initValue;
        }
        prompt = StringUtils.isEmpty(prompt) ? getPromptString(attribute) : prompt;
        return textIO.<String>newGenericInputReader(null)
                .withNumberedPossibleValues(options).withDefaultValue(defaultValue).withEqualsFunc((s1, s2) -> s1.equalsIgnoreCase(s2)).read(prompt);
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, String regex,
                                      String prompt, String errorMessage) throws MojoFailureException {
        final String initValue = getInitValue(attribute);
        if (initValue != null && validateInputByRegex(initValue, regex)) {
            Log.info(FOUND_VALID_VALUE);
            return initValue;
        }

        prompt = StringUtils.isEmpty(prompt) ? getPromptString(attribute) : prompt;
        return textIO.newStringInputReader().withPattern(regex).withDefaultValue(defaultValue).withMinLength(0).read(prompt);
    }

    private String getPromptString(String attributeName) {
        return String.format(PROMPT_STRING_WITHOUT_DEFAULTVALUE, attributeName);
    }

    @Override
    public void close() {
        textIO.dispose();
    }
}
