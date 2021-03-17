/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils.aspect;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
import org.apache.commons.lang3.StringUtils;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class ExpressionUtils {
    private static final Uri2NameFunction toName = new Uri2NameFunction();
    private static final EnvironmentConfiguration jgConfig = EnvironmentConfigurationBuilder.configuration().functions().add(toName).and().build();
    private static final ImmutableMap<String, Boolean> valueMap = ImmutableMap.of("true", true, "false", false);

    public static boolean evaluate(String expression, MethodInvocation invocation, boolean defaultVal) {
        final String result = interpret(expression, invocation);
        return valueMap.getOrDefault(result.toLowerCase(), defaultVal);
    }

    public static String interpret(String expression, MethodInvocation invocation) {
        return render(String.format("{{%s}}", expression), invocation);
    }

    public static String render(String expressionOrTemplate, MethodInvocation invocation) {
        //TODO: validate&decide expression or template
        final boolean isTemplate = expressionOrTemplate.contains("{{") && expressionOrTemplate.contains("}}");
        final boolean hasExpression = StringUtils.containsAny(expressionOrTemplate, "@", "$");
        if (isTemplate) {
            assert hasExpression : "invalid jtwig template";
            return render(expressionOrTemplate, toMap(invocation));
        } else if (hasExpression) {
            return render(String.format("{{%s}}", expressionOrTemplate), toMap(invocation));
        } else {
            return expressionOrTemplate;
        }
    }

    private static String render(String template, Map<String, Object> variables) {
        final String fixedTemplate = template.replace("@", "$$.");
        final JtwigTemplate tpl = JtwigTemplate.inlineTemplate(fixedTemplate, jgConfig);
        final JtwigModel model = JtwigModel.newModel();
        variables.forEach(model::with);
        return tpl.render(model);
    }

    @Nonnull
    private static Map<String, Object> toMap(@Nonnull final MethodInvocation invocation) {
        final String[] paramNames = invocation.getParamNames();
        final Object[] paramValues = invocation.getParamValues();
        final Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            variables.put("$" + paramNames[i], paramValues[i]);
        }
        variables.put("$$", invocation.getInstance());
        return variables;
    }

    private static class Uri2NameFunction extends SimpleJtwigFunction {

        @Override
        public String name() {
            return "uri_to_name";
        }

        @Override
        public Object execute(FunctionRequest request) {
            final String input = getInput(request);
            return ResourceUtils.nameFromResourceId(input);
        }

        private String getInput(FunctionRequest request) {
            request.minimumNumberOfArguments(1).maximumNumberOfArguments(1);
            return request.getEnvironment().getValueEnvironment().getStringConverter().convert(request.get(0));
        }
    }
}
