/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.queryer;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;
import java.util.Scanner;

import static java.lang.System.out;

public class MavenPluginQueryerDefaultImpl extends MavenPluginQueryer {

    public static final String FOUND_VALID_VALUE = "Found valid value. Skip user input.";
    public static final String PROMPT_STRING_WITH_DEFAULTVALUE = "Define value for %s(Default: %s): ";
    public static final String PROMPT_STRING_WITHOUT_DEFAULTVALUE = "Define value for %s: ";
    public static final String DEFAULT_INPUT_ERROR_MESSAGE = "Invalid input, please check and try again.";
    public static final String DEFAULT_INPUT_ERROR_MESSAGE_WITH_REGEX = "Invalid input, value should match( %s ).";

    private Scanner reader;
    private Log log;

    public MavenPluginQueryerDefaultImpl(Log log) {
        this.log = log;
        this.reader = new Scanner(System.in);
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, List<String> options, String prompt) {
        final String initValue = getInitValue(attribute);
        if (initValue != null && validateInputByOptions(initValue, options)) {
            log.info(FOUND_VALID_VALUE);
            return initValue;
        }
        prompt = StringUtils.isEmpty(prompt) ? getPromptString(attribute, defaultValue) : prompt;
        out.println(prompt);
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i) != null && options.get(i).equalsIgnoreCase(defaultValue)) {
                out.println(String.format("%d. %s [*]", i + 1, options.get(i)));
            } else {
                out.println(String.format("%d. %s", i + 1, options.get(i)));
            }
        }
        while (true) {
            out.print("Enter index to use: ");
            out.flush();
            try {
                final String input = reader.nextLine();
                if (StringUtils.isEmpty(input) && validateInputByOptions(defaultValue, options)) {
                    return defaultValue;
                }
                final int choice = Integer.parseInt(input);
                if (choice > 0 && choice <= options.size()) {
                    return options.get(choice - 1);
                }
            } catch (NumberFormatException e) {
                // Swallow this exception
            }
            out.println("Invalid index.");
        }
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, String regex,
                                      String prompt, String errorMessage) throws MojoFailureException {
        final String initValue = getInitValue(attribute);
        if (initValue != null && validateInputByRegex(initValue, regex)) {
            log.info(FOUND_VALID_VALUE);
            return initValue;
        }

        while (true) {
            prompt = StringUtils.isEmpty(prompt) ? getPromptString(attribute, defaultValue) : prompt;
            out.print(prompt);
            out.flush();
            String input = null;
            input = reader.nextLine();
            if (StringUtils.isNotEmpty(defaultValue) && StringUtils.isEmpty(input)) {
                return defaultValue;
            } else if (validateInputByRegex(input, regex)) {
                return input;
            }
            errorMessage = StringUtils.isEmpty(errorMessage) ? getErrorMessage(regex) : errorMessage;
            out.println(errorMessage);
        }
    }

    private String getPromptString(String attributeName, String defaultValue) {
        return StringUtils.isBlank(defaultValue) ?
            String.format(PROMPT_STRING_WITHOUT_DEFAULTVALUE, attributeName) :
            String.format(PROMPT_STRING_WITH_DEFAULTVALUE, attributeName, defaultValue);
    }

    private String getErrorMessage(String regex) {
        return StringUtils.isEmpty(regex) ? DEFAULT_INPUT_ERROR_MESSAGE :
            String.format(DEFAULT_INPUT_ERROR_MESSAGE_WITH_REGEX, regex);
    }

    @Override
    public void close() {
        reader.close();
    }

}
