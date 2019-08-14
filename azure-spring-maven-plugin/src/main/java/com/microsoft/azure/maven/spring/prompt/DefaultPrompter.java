/**
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.prompt;

import com.microsoft.azure.maven.spring.exception.NoResourcesAvailableException;
import com.microsoft.azure.maven.utils.TextUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
    private static final String EMPTY_REPLACEMENT = ":";
    private static final String PROMPT_MESSAGE_INDEX_TEMPLATE = "Enter an index value for %s %s: ";
    private static final String PROMPT_MESSAGE_BAD_INDEX_TEMPLATE = "You have input a wrong number (%s).%nPlease enter an index value for %s again %s: ";
    private static final String PROMPT_MESSAGE_NUM_TEMPLATE = "You have input a wrong number (%s).%nPlease enter a number for %s again %s: ";
    private static final String PROMPT_MESSAGE_STRING_TEMPLATE = "You have input a wrong value (%s).%nPlease input %s again %s: ";

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
    private static final Pattern ANY_STRING_REGEX = Pattern.compile(".*");

    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public String promoteString(String name, String defaultValue, String regex, boolean isRequired) throws IOException {
        final boolean hasDefaultValue = StringUtils.isNotBlank(defaultValue);
        String hintMessage = hasDefaultValue ? TextUtils.green(defaultValue) : "";
        if (!isRequired && hasDefaultValue) {
            hintMessage = hintMessage + ", press '" + EMPTY_REPLACEMENT + "' to input empty";
        }
        if (StringUtils.isNotBlank(hintMessage)) {
            hintMessage = "(" + hintMessage + ")";
        }
        System.out.printf("Please input the %s%s: ", name, hintMessage);
        System.out.flush();
        final String finalHintMessage = hintMessage;
        final Pattern pattern = StringUtils.isNotBlank(regex) ? Pattern.compile(regex) : ANY_STRING_REGEX;
        return loopInput(defaultValue, hasDefaultValue, isRequired, String.format("Please input the %s%s: ", name, hintMessage), input -> {
            if (!isRequired && StringUtils.equals(EMPTY_REPLACEMENT, input.trim())) {
                return "";
            }
            if (pattern.matcher(input).find()) {
                return input;
            }
            throw new UserInputException(String.format(PROMPT_MESSAGE_STRING_TEMPLATE, input, name, finalHintMessage));
        });
    }

    public Boolean promoteYesNo(Boolean defaultValue, String message, boolean isRequired) throws IOException {
        final boolean hasDefaultValue = defaultValue != null;

        if (!isRequired && hasDefaultValue) {
            throw new IllegalArgumentException("There is no way to input empty value for a non-required field with default value.");
        }
        final String hintMessage = yesOrNo(defaultValue);
        System.out.printf("%s %s: ", message, hintMessage);
        System.out.flush();

        return loopInput(defaultValue, hasDefaultValue, isRequired, String.format("%s %s: ", message, hintMessage), input -> {
            if (input.equalsIgnoreCase("Y")) {
                return Boolean.TRUE;
            }
            if (input.equalsIgnoreCase("N")) {
                return Boolean.FALSE;
            }
            throw new UserInputException(String.format("Invalid input (%s).%n%s %s: ", input, message, hintMessage));
        });
    }

    public <T> List<T> promoteMultipleEntities(String name, List<T> entities, Function<T, String> getNameFunc, boolean allowEmpty,
            String enterPromote, List<T> defaultValue) throws IOException {
        final boolean hasDefaultValue = defaultValue != null && defaultValue.size() > 0;
        List<T> res = new ArrayList<>();
        int index = 1;

        if (!allowEmpty && entities.size() == 1) {
            return entities;
        }
        System.out.println(String.format("Please select values for %ss: ", name));
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
                if (hasDefaultValue) {
                    return defaultValue;
                }
                if (allowEmpty) {
                    return res;
                }
                System.out.print(String.format("You have not select any %ss.%nPlease enter index values separated by comma%s: ", name, hintMessage));
            }
            if (isValidIntRangeInput(input)) {
                for (final int i : parseIntRanges(input, 1, entities.size())) {
                    res.add(entities.get(i - 1));
                }
                res = res.stream().distinct().collect(Collectors.toList());
                if (res.size() > 0 || allowEmpty) {
                    return res;
                }
                System.out.print(String.format("You have not select any %ss.%nPlease enter index values separated by comma%s: ", name, hintMessage));
            } else {
                System.out.print(String.format("The input value('%s') cannot be recognized.%n" + "Please enter index values separated by comma%s: ",
                        input, hintMessage));
            }

            System.out.flush();
        }
    }

    public <T> T promoteSingleEntity(String name, List<T> entities, T defaultEntity, Function<T, String> getNameFunc, boolean isRequired)
            throws IOException, NoResourcesAvailableException {
        final boolean hasDefaultValue = defaultEntity != null;
        int index = 1;
        if (entities.size() == 0) {
            throw new NoResourcesAvailableException(String.format("No %ss are found.", name));
        }
        if (entities.size() == 1) {
            System.out.println(String.format("Use %s (%s) automatically.", name, getNameFunc.apply(entities.get(0))));
            return entities.get(0);
        }

        System.out.println(String.format("Please select a %s: ", name));
        for (final T entity : entities) {
            final String displayLine = String.format("%2d. %s", index++, getNameFunc.apply(entity));
            System.out.println(defaultEntity == entity ? TextUtils.blue(displayLine) : displayLine);
        }

        final int selectedIndex = entities.indexOf(defaultEntity);

        final String defaultValueMessage = selectedIndex >= 0 ? " (" + TextUtils.blue(new Integer(selectedIndex + 1).toString()) + ")" : "";
        final String hintMessage = String.format("[1-%d]%s", entities.size(), defaultValueMessage);
        System.out.printf(PROMPT_MESSAGE_INDEX_TEMPLATE, name, hintMessage);
        System.out.flush();

        return loopInput(defaultEntity, hasDefaultValue, isRequired, String.format(PROMPT_MESSAGE_INDEX_TEMPLATE, name, hintMessage), input -> {
            final Integer selectIndex = validateUserInputAsInteger(input, 1, entities.size(),
                    String.format(PROMPT_MESSAGE_BAD_INDEX_TEMPLATE, input, name, hintMessage));
            return entities.get(selectIndex - 1);
        });
    }

    public Integer promoteInteger(String name, Integer defaultValue, int minValue, int maxValue, boolean isRequired) throws IOException {
        final boolean hasDefaultValue = defaultValue != null;
        if (!isRequired && hasDefaultValue) {
            throw new IllegalArgumentException("There is no way to input empty value for a non-required field with default value.");
        }
        final String defaultValueMessage = hasDefaultValue ? " (" + TextUtils.blue(defaultValue.toString()) + ")" : "";
        final String hintMessage = String.format("[%d-%d]%s", minValue, maxValue, defaultValueMessage);
        final String message = String.format("Please input the %s %s: ", name, hintMessage);
        System.out.print(message);
        System.out.flush();

        return loopInput(defaultValue, hasDefaultValue, isRequired, message, input -> {
            return validateUserInputAsInteger(input, minValue, maxValue, String.format(PROMPT_MESSAGE_NUM_TEMPLATE, input, name, hintMessage));
        });
    }

    static class UserInputException extends RuntimeException {
        private static final long serialVersionUID = 7986173597822766589L;

        UserInputException(String message) {
            super(message);
        }
    }

    private <T> T loopInput(T defaultValue, boolean hasDefaultValue, boolean isRequired, String emptyPromoteMessage,
            Function<String, T> handleInput) throws IOException {
        while (true) {
            final String input = reader.readLine();
            if (StringUtils.isBlank(input)) {
                if (hasDefaultValue || !isRequired) {
                    return defaultValue;
                }
                System.out.print(emptyPromoteMessage);
            } else {
                try {
                    return handleInput.apply(input);
                } catch (UserInputException ex) {
                    System.out.print(ex.getMessage());
                }
            }
            System.out.flush();
        }
    }

    private Integer validateUserInputAsInteger(String input, int start, int end, String message) {
        if (!NumberUtils.isDigits(input)) {
            throw new UserInputException(message);
        }

        final int value = Integer.parseInt(input);
        if (value >= start && value <= end) {
            return value;
        }

        throw new UserInputException(message);
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
        return REGEX_COMMA_SEPARATED_INTEGER_RANGES.matcher(text).matches();
    }

    private static int[] parseIntRanges(String text, int minValue, int maxValue) {
        final Matcher m = REGEX_NEXT_INTEGER_RANGE.matcher(text);
        final List<Integer> values = new ArrayList<>();
        while (m.find()) {
            final int s1 = Math.max(minValue, Integer.parseInt(m.group(1)));

            if (m.group(2) != null) {
                // use maxValue to avoid very large enumeration like 1-2^32
                final int s2 = Math.min(maxValue, Integer.parseInt(m.group(2)));
                for (int i = Math.max(minValue, s1); i <= s2; i++) {
                    values.add(i);
                }
            } else {
                if (s1 <= maxValue) {
                    values.add(s1);
                }

            }
        }
        return ArrayUtils.toPrimitive(values.toArray(new Integer[0]));
    }
}
