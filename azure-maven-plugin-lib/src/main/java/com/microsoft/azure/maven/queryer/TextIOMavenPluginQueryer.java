/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.queryer;

import com.microsoft.azure.maven.utils.TextIOUtils;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import org.apache.commons.lang3.StringUtils;
import org.beryx.textio.GenericInputReader;
import org.beryx.textio.StringInputReader;

import java.util.List;

public class TextIOMavenPluginQueryer extends MavenPluginQueryer {
    private static final String FOUND_VALID_VALUE = "Found valid value. Skip user input.";
    private static final String PROMPT_STRING_WITHOUT_DEFAULTVALUE = "Define value for %s";

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, List<String> options, String prompt) {
        final String initValue = getInitValue(attribute);
        if (initValue != null && validateInputByOptions(initValue, options)) {
            Log.info(FOUND_VALID_VALUE);
            return initValue;
        }
        prompt = StringUtils.isEmpty(prompt) ? getPromptString(attribute) : prompt;
        return new GenericInputReader<String>(TextIOUtils::getTextTerminal, null)
                .withNumberedPossibleValues(options).withDefaultValue(defaultValue).withEqualsFunc(StringUtils::equalsIgnoreCase).read(prompt);
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, String regex,
                                      String prompt, String errorMessage) {
        final String initValue = getInitValue(attribute);
        if (initValue != null && validateInputByRegex(initValue, regex)) {
            Log.info(FOUND_VALID_VALUE);
            return initValue;
        }

        prompt = StringUtils.isEmpty(prompt) ? getPromptString(attribute) : prompt;
        return new StringInputReader(TextIOUtils::getTextTerminal).withPattern(regex).withDefaultValue(defaultValue).withMinLength(0).read(prompt);
    }

    private String getPromptString(String attributeName) {
        return String.format(PROMPT_STRING_WITHOUT_DEFAULTVALUE, attributeName);
    }

    @Override
    public void close() {
    }
}
