/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.maven.function.FunctionConfiguration;
import com.microsoft.azure.serverless.functions.annotation.*;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AnnotationHandlerImplTest {
    public static final String HTTP_TRIGGER_FUNCTION = "HttpTriggerFunction";
    public static final String HTTP_TRIGGER_METHOD = "httpTriggerMethod";
    public static final String QUEUE_TRIGGER_FUNCTION = "QueueTriggerFunction";
    public static final String QUEUE_TRIGGER_METHOD = "queueTriggerMethod";
    public static final String TIMER_TRIGGER_FUNCTION = "TimerTriggerFunction";
    public static final String TIMER_TRIGGER_METHOD = "timerTriggerMethod";

    public class FunctionEntryPoints {
        @FunctionName(HTTP_TRIGGER_FUNCTION)
        public String httpTriggerMethod(@HttpTrigger(name = "req") String req) {
            return "Hello!";
        }

        @FunctionName(QUEUE_TRIGGER_FUNCTION)
        public void queueTriggerMethod(@QueueTrigger(name = "in", queueName = "qIn", connection = "conn") String in,
                                       @QueueOutput(name = "out", queueName = "qOut", connection = "conn") String out) {
        }

        @FunctionName(TIMER_TRIGGER_FUNCTION)
        public void timerTriggerMethod(@TimerTrigger(name = "timer", schedule = "", useMonitor = false) String timer) {
        }
    }

    @Test
    public void findFunctions() throws Exception {
        final Log log = mock(Log.class);
        final AnnotationHandler handler = new AnnotationHandlerImpl(log);

        final URL classURL = ClasspathHelper.forPackage("com.microsoft.azure.maven.function.handlers")
                .iterator()
                .next();
        final Set<Method> functions = handler.findFunctions(classURL);
        assertEquals(3, functions.size());
        final List<String> methodNames = functions.stream().map(f -> f.getName()).collect(Collectors.toList());
        assertTrue(methodNames.contains(HTTP_TRIGGER_METHOD));
        assertTrue(methodNames.contains(QUEUE_TRIGGER_METHOD));
        assertTrue(methodNames.contains(TIMER_TRIGGER_METHOD));
    }

    @Test
    public void generateConfigurations() throws Exception {
        final Log log = mock(Log.class);
        final AnnotationHandler handler = new AnnotationHandlerImpl(log);

        final URL classURL = ClasspathHelper.forPackage("com.microsoft.azure.maven.function.handlers")
                .iterator()
                .next();
        final Set<Method> functions = handler.findFunctions(classURL);
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(functions);

        assertEquals(3, configMap.size());
        assertTrue(configMap.containsKey(HTTP_TRIGGER_FUNCTION));
        final FunctionConfiguration httpTriggerFunctionConfig = configMap.get(HTTP_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(HTTP_TRIGGER_METHOD), httpTriggerFunctionConfig.getEntryPoint());
        assertFalse(httpTriggerFunctionConfig.isDisabled());
        assertEquals(2, httpTriggerFunctionConfig.getBindings().size());

        assertTrue(configMap.containsKey(QUEUE_TRIGGER_FUNCTION));
        final FunctionConfiguration queueTriggerFunctionConfig = configMap.get(QUEUE_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(QUEUE_TRIGGER_METHOD), queueTriggerFunctionConfig.getEntryPoint());
        assertEquals(2, queueTriggerFunctionConfig.getBindings().size());

        assertTrue(configMap.containsKey(TIMER_TRIGGER_FUNCTION));
        final FunctionConfiguration timerTriggerFunctionConfig = configMap.get(TIMER_TRIGGER_FUNCTION);
        assertEquals(getFullyQualifiedMethodName(TIMER_TRIGGER_METHOD), timerTriggerFunctionConfig.getEntryPoint());
        assertEquals(1, timerTriggerFunctionConfig.getBindings().size());
    }

    private String getFullyQualifiedMethodName(final String methodName) {
        return FunctionEntryPoints.class.getCanonicalName() + "." + methodName;
    }
}
