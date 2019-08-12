/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.prompt;

import com.microsoft.azure.maven.spring.exception.NoResourcesAvailableException;
import com.microsoft.azure.maven.utils.TextUtils;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DefaultPrompter implements Closeable {
    private static final String PROMOT_MESSAGE_INDEX_TEMPLATE = "Enter an index value for %s %s: ";
    private static final String PROMOT_MESSAGE_BAD_INDEX_TEMPLATE = "You have input a wrong number(%s), enter an index value for %s again %s: ";
    private static final String PROMOT_MESSAGE_NUM_TEMPLATE = "You have input a wrong number(%s), enter a number for %s again %s: ";
    private static final String PROMOT_MESSAGE_STRING_TEMPLATE = "You have input a wrong value(%s), please input %s again %s: ";
    private static final int MAX_ITEMS = 10000;

    // this code is copied from https://stackoverflow.com/questions/13011657/advanced-parsing-of-numeric-ranges-from-string
    // the author of it is: https://stackoverflow.com/users/433790/ridgerunner
    private static final Pattern REGEX_COMMA_SEPARATED_INTEGER_RANGES = Pattern.compile("# Validate comma separated integers/integer ranges.\n" +
            "^             # Anchor to start of string.         \n" +
            "[0-9]+        # Integer of 1st value (required).   \n" +
            "(?:           # Range for 1st value (optional).    \n" +
            "  -           # Dash separates range integer.      \n" +
            "  [0-9]+      # Range integer of 1st value.        \n" +
            ")?            # Range for 1st value (optional).    \n" +
            "(?:           # Zero or more additional values.    \n" +
            "  ,           # Comma separates additional values. \n" +
            "  [0-9]+      # Integer of extra value (required). \n" +
            "  (?:         # Range for extra value (optional).  \n" +
            "    -         # Dash separates range integer.      \n" +
            "    [0-9]+    # Range integer of extra value.      \n" +
            "  )?          # Range for extra value (optional).  \n" +
            ")*            # Zero or more additional values.    \n" +
            "$             # Anchor to end of string.           ", Pattern.COMMENTS);

    private static final Pattern REGEX_NEXT_INTEGER_RANGE = Pattern.compile("# extract next integers/integer range value.    \n" +
            "([0-9]+)      # $1: 1st integer (Base).         \n" +
            "(?:           # Range for value (optional).     \n" +
            "  -           # Dash separates range integer.   \n" +
            "  ([0-9]+)    # $2: 2nd integer (Range)         \n" +
            ")?            # Range for value (optional). \n" +
            "(?:,|$)       # End on comma or string end.", Pattern.COMMENTS);

    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public String promoteString(String name, String defaultValue, String regex, boolean isRequired) throws IOException {
        final boolean hasDefaultValue = StringUtils.isNotBlank(defaultValue);
        final String hintMessage = hasDefaultValue ? (" (" + TextUtils.green(defaultValue) + ")") : "";
        System.out.printf("Please input the %s%s:", name, hintMessage);
        System.out.flush();

        while (true) {

            final String input = reader.readLine();
            if (StringUtils.isBlank(input)) {
                if (hasDefaultValue || !isRequired) {
                    return defaultValue;
                }
            }
            if (Pattern.compile(regex).matcher(input).find()) {
                return input;
            } else {
                System.out.printf(PROMOT_MESSAGE_STRING_TEMPLATE, input, name, hintMessage);
                System.out.flush();
            }
        }
    }

    public Boolean promoteYesNo(Boolean defaultValue, String message) throws IOException {
        final String hintMessage = yesOrNo(defaultValue);
        System.out.printf("%s %s: ", message, hintMessage);
        System.out.flush();
        while (true) {
            final String input = reader.readLine();
            if (StringUtils.isBlank(input)) {
                return defaultValue;
            }
            if (input.equalsIgnoreCase("Y")) {
                return true;
            }
            if (input.equalsIgnoreCase("N")) {
                return false;
            }

            System.out.printf("Invalid input(%s), %s %s: ", input, message, hintMessage);
            System.out.flush();
        }
    }

    public <T> List<T> promoteManyEntities(String name, List<T> entities, Function<T, String> getNameFunc, boolean allowEmpty, String enterPromote,
            List<T> defaultList) throws IOException {
        List<T> res = new ArrayList<>();
        int index = 1;

        if (!allowEmpty && entities.size() == 1) {
            return entities;
        }
        System.out.println(String.format("Please select values for %ss:", name));
        for (final T entity : entities) {
            final String displayLine = String.format("%2d. %s", index++, getNameFunc.apply(entity));
            System.out.println(displayLine);
        }
        final String hintMessage = String.format("(eg: %s, press %s %s)", TextUtils.blue("[1-2,4,4]"), TextUtils.blue("ENTER"), enterPromote);
        System.out.print(String.format("Enter index values separated by comma%s: ", hintMessage));
        System.out.flush();
        for (;;) {

            final String input = reader.readLine();
            if (StringUtils.isBlank(input)) {
                return defaultList;
            }
            if (isValidIntRangeInput(input)) {
                for (final int i : parseIntRanges(input, MAX_ITEMS)) {
                    if (i >= 1 && i <= entities.size()) {
                        res.add(entities.get(i - 1));
                    }
                }
                res = res.stream().distinct().collect(Collectors.toList());
                if (res.size() > 0 || allowEmpty) {
                    return res;
                }
                System.out.print(String.format("You have not select any %ss, please enter the index values again%s: ", name, hintMessage));
            } else {
                System.out.print(String.format("Value('%s') cannot be recognized, please enter the index values again%s: ", input, hintMessage));
            }

            System.out.flush();
        }

    }

    public <T> T promoteSingleEntity(String name, List<T> entities, T defaultEntity, Function<T, String> getNameFunc)
            throws IOException, NoResourcesAvailableException {
        int index = 1;
        if (entities.size() == 0) {
            throw new NoResourcesAvailableException(String.format("No %ss are found.", name));
        }
        if (entities.size() == 1) {
            System.out.println(String.format("Use %s (%s) automatically.", name, getNameFunc.apply(entities.get(0))));
            return entities.get(0);
        }

        System.out.println(String.format("Please select a %s:", name));
        for (final T entity : entities) {
            final String displayLine = String.format("%2d. %s", index++, getNameFunc.apply(entity));
            System.out.println(defaultEntity == entity ? TextUtils.blue(displayLine) : displayLine);
        }

        final int selectedIndex = entities.indexOf(defaultEntity);

        final String defaultValueMessage = selectedIndex >= 0 ? " (" + TextUtils.blue(new Integer(selectedIndex + 1).toString()) + ")" : "";
        final String hintMessage = String.format("[1-%d]%s", entities.size(), defaultValueMessage);
        System.out.printf(PROMOT_MESSAGE_INDEX_TEMPLATE, name, hintMessage);
        System.out.flush();
        while (true) {
            final String input = reader.readLine();

            if (StringUtils.isBlank(input)) {
                if (defaultEntity != null) {
                    return defaultEntity;
                }
                System.out.printf(PROMOT_MESSAGE_INDEX_TEMPLATE, name, hintMessage);
            } else {
                final Integer selectIndex = validateUserInputAsInteger(input, 1, entities.size(),
                        String.format(PROMOT_MESSAGE_BAD_INDEX_TEMPLATE, input, name, hintMessage));
                if (selectIndex != null) {
                    return entities.get(selectIndex - 1);
                }
            }
            System.out.flush();
        }
    }

    public Integer promoteInteger(String name, Integer defaultValue, int minValue, int maxValue, boolean isRequired) throws IOException {
        final boolean hasDefaultValue = defaultValue != null;
        final String defaultValueMessage = hasDefaultValue ? " (" + TextUtils.blue(defaultValue.toString()) + ")" : "";
        final String hintMessage = String.format("[%d-%d]%s", minValue, maxValue, defaultValueMessage);
        final String message = String.format("Please input the %s %s:", name, hintMessage);
        System.out.print(message);
        System.out.flush();
        while (true) {

            final String input = reader.readLine();
            if (StringUtils.isBlank(input)) {
                if (hasDefaultValue || !isRequired) {
                    return defaultValue;
                }
                System.out.print(message);
            } else {
                final Integer value = validateUserInputAsInteger(input, minValue, maxValue,
                        String.format(PROMOT_MESSAGE_NUM_TEMPLATE, input, name, hintMessage));
                if (value != null) {
                    return value;
                }

            }
            System.out.flush();
        }
    }

    private Integer validateUserInputAsInteger(String input, int start, int end, String message) {
        if (!NumberUtils.isDigits(input)) {
            System.out.print(message);
            return null;
        }

        final int value = Integer.parseInt(input);
        if (value >= start && value <= end) {
            return value;
        }

        System.out.print(message);
        return null;
    }

    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            // swallow this error
        }
    }

    private static String yesOrNo(Boolean defaultValue) {
        if (defaultValue == null) {
            return "(y/n)";
        }
        if (defaultValue.booleanValue()) {
            return "(Y/n)";
        }
        return "(y/N)";
    }

    private static boolean isValidIntRangeInput(String text) {
        final Matcher m = REGEX_COMMA_SEPARATED_INTEGER_RANGES.matcher(text);
        return m.matches();
    }

    private static int[] parseIntRanges(String text, int maxValue) {
        final Matcher m = REGEX_NEXT_INTEGER_RANGE.matcher(text);
        final List<Integer> values = new ArrayList<>();
        while (m.find()) {
            final int s1 = Integer.parseInt(m.group(1));

            if (m.group(2) != null) {
                // use maxValue to avoid very large enumeration like 1-2^32
                final int s2 = Math.min(maxValue, Integer.parseInt(m.group(2)));
                for (int i = s1; i <= s2; i++) {
                    values.add(i);
                }
            } else {
                values.add(s1);
            }
        }
        return ArrayUtils.toPrimitive(values.toArray(new Integer[0]));
    }
}
