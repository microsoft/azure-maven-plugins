package com.microsoft.azure.maven.queryer;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

public class MavenPluginQueryerDefaultImpl extends MavenPluginQueryer {

    public static final String CLOSE_FAILURE_WARNING = "Can't close io stream for queryer.";
    public static final String FOUND_VALID_VALUE = "Found valid value. Skip user input.";
    public static final String PROMPT_STRING_WITH_DEFAULTVALUE = "Enter value for %s(Default: %s): ";
    public static final String PROMPT_STRING_WITHOUT_DEFAULTVALUE = "Enter value for %s: ";
    public static final String PROMPT_STRING_FOR_OPTION_WITH_DEFAULTVALUE = "Choose from below options as the value " +
        "of %s(Default %s)";
    public static final String PROMPT_STRING_FOR_OPTION_WITHOUT_DEFAULTVALUE = "Choose from below options as the " +
        "value of %s";
    public static final String DEFAULT_INPUT_ERROR_MESSAGE = "Invalid input, please check and try again.";

    private BufferedReader reader;
    private PrintWriter writer;
    private Log log;

    public MavenPluginQueryerDefaultImpl(Log log) {
        this(System.in, System.out, log);
    }

    public MavenPluginQueryerDefaultImpl(InputStream inputStream, OutputStream outputStream, Log log) {
        this.log = log;
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.writer = new PrintWriter(new OutputStreamWriter(outputStream), true);
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, List<String> options, String prompt)
        throws MojoFailureException {
        final String initValue = getInitValue(attribute);
        if (initValue != null && validateInputByOptions(initValue, options)) {
            return initValue;
        }
        prompt = StringUtils.isEmpty(prompt) ? getPromptStringWithOptions(attribute, defaultValue) : prompt;
        writer.println(prompt);
        for (int i = 0; i < options.size(); i++) {
            writer.println(String.format("%d. %s", i, options.get(i)));
        }
        while (true) {
            writer.print("Enter index to use: ");
            writer.flush();
            try {
                final String input = reader.readLine();
                if (StringUtils.isEmpty(input) && validateInputByOptions(defaultValue, options)) {
                    return defaultValue;
                }
                final int choice = Integer.parseInt(input);
                if (choice >= 0 && choice < options.size()) {
                    return options.get(choice);
                }
            } catch (NumberFormatException e) {
                // Sallow this exception
            } catch (IOException e) {
                throw new MojoFailureException("Can't get input from user.", e);
            }
            writer.println("Invalid index.");
        }
    }

    private String getPromptStringWithOptions(String attributeName, String defaultValue) {
        return StringUtils.isBlank(defaultValue) ?
            String.format(PROMPT_STRING_FOR_OPTION_WITHOUT_DEFAULTVALUE, attributeName) :
            String.format(PROMPT_STRING_FOR_OPTION_WITH_DEFAULTVALUE, attributeName, defaultValue);
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
            writer.print(prompt);
            writer.flush();
            String input = null;
            try {
                input = reader.readLine();
                if (StringUtils.isNotEmpty(defaultValue) && StringUtils.isEmpty(input)) {
                    return defaultValue;
                } else if (validateInputByRegex(input, regex)) {
                    return input;
                }
                errorMessage = StringUtils.isEmpty(errorMessage) ? DEFAULT_INPUT_ERROR_MESSAGE : errorMessage;
                writer.println(errorMessage);
            } catch (IOException e) {
                throw new MojoFailureException("Can't get input from user.", e);
            }
        }
    }

    private String getPromptString(String attributeName, String defaultValue) {
        return StringUtils.isBlank(defaultValue) ?
            String.format(PROMPT_STRING_WITHOUT_DEFAULTVALUE, attributeName) :
            String.format(PROMPT_STRING_WITH_DEFAULTVALUE, attributeName, defaultValue);
    }

    @Override
    public void close() {
        try {
            reader.close();
            writer.close();
        } catch (IOException e) {
            log.warn(CLOSE_FAILURE_WARNING);
        }
    }

}
