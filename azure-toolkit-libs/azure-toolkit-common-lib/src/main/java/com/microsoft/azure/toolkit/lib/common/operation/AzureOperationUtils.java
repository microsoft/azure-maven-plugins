/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AzureOperationUtils {
    private static final Uri2NameFunction toName = new Uri2NameFunction();
    public static final EnvironmentConfiguration jgConfig = EnvironmentConfigurationBuilder.configuration().functions().add(toName).and().build();

    public static AzureOperation getAnnotation(@Nonnull AzureOperationRef ref) {
        final Method method = ref.getMethod();
        return method.getAnnotation(AzureOperation.class);
    }

    public static String getOperationTitle(@Nonnull AzureOperationRef ref) {
        final AzureOperation annotation = AzureOperationUtils.getAnnotation(ref);
        final String name = annotation.name();
        final String[] params = Arrays.stream(annotation.params()).map(e -> interpretExpression(e, ref)).toArray(String[]::new);
        return AzureOperationBundle.title(name, (Object[]) params).toString();
    }

    private static String interpretExpression(String expression, AzureOperationRef ref) {
        final String[] paramNames = ref.getParamNames();
        final Object[] paramValues = ref.getParamValues();
        final Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            variables.put("$" + paramNames[i], paramValues[i]);
        }
        variables.put("$$", ref.getInstance());
        final String fixedExpression = expression.replace("@", "$$.");
        return interpretInline(fixedExpression, variables);
    }

    private static String interpretInline(String expr, Map<String, Object> variables) {
        final JtwigTemplate template = JtwigTemplate.inlineTemplate(String.format("{{%s}}", expr), jgConfig);
        final JtwigModel model = JtwigModel.newModel();
        variables.forEach(model::with);
        return template.render(model);
    }

    /**
     * get all ancestors until the last operation of ACTION type.
     */
    public static List<IAzureOperation> revise(Deque<? extends IAzureOperation> ops) {
        final LinkedList<IAzureOperation> result = new LinkedList<>();
        for (final IAzureOperation op : ops) {
            result.addFirst(op);
            if (op instanceof AzureOperationRef) {
                final AzureOperation annotation = AzureOperationUtils.getAnnotation((AzureOperationRef) op);
                if (annotation.type() == AzureOperation.Type.ACTION) {
                    break;
                }
            }
        }
        return result;
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
