package com.microsoft.azure.maven.queryer;

import com.microsoft.azure.maven.Utils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;

public class MavenPluginQueryerBatchModeDefaultImpl extends MavenPluginQueryer {

    private Log log;

    public MavenPluginQueryerBatchModeDefaultImpl(Log log) {
        this.log = log;
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, List<String> options) throws MojoFailureException {
        final String initValue = Utils.getSystemProperty(attribute);
        final String input = StringUtils.isNotEmpty(initValue) ? initValue : defaultValue;
        if (validateInputByOptions(input, options)) {
            log.info(String.format("Use %s for %s", input, attribute));
            return input;
        }
        throw new MojoFailureException(String.format("Invalid input for %s : %s", attribute, input));
    }

    @Override
    public String assureInputFromUser(String attribute, String defaultValue, String regex, String errorMessage) throws MojoFailureException {
        final String initValue = Utils.getSystemProperty(attribute);
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
