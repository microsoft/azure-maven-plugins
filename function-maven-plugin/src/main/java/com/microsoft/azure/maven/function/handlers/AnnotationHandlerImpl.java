/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.maven.function.FunctionConfiguration;
import com.microsoft.azure.maven.function.bindings.BaseBinding;
import com.microsoft.azure.maven.function.bindings.HttpBinding;
import com.microsoft.azure.maven.function.bindings.QueueBinding;
import com.microsoft.azure.maven.function.bindings.TimerBinding;
import com.microsoft.azure.serverless.functions.annotation.*;
import org.apache.maven.plugin.logging.Log;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class AnnotationHandlerImpl implements AnnotationHandler {
    protected Log log;

    public AnnotationHandlerImpl(final Log log) {
        this.log = log;
    }

    @Override
    public Set<Method> findFunctions(final URL url) {
        return new Reflections(
                new ConfigurationBuilder()
                        .addUrls(url)
                        .addScanners(new MethodAnnotationsScanner())
                        .addClassLoader(getClassLoader(url)))
                .getMethodsAnnotatedWith(FunctionName.class);
    }

    protected ClassLoader getClassLoader(final URL url) {
        return new URLClassLoader(new URL[]{url}, this.getClass().getClassLoader());
    }

    @Override
    public Map<String, FunctionConfiguration> generateConfigurations(final Set<Method> methods) throws Exception {
        final Map<String, FunctionConfiguration> configMap = new HashMap<>();
        for (final Method method : methods) {
            final FunctionName functionAnnotation = method.getAnnotation(FunctionName.class);
            log.debug("Starting processing function : " + functionAnnotation.value());
            configMap.put(functionAnnotation.value(), generateConfiguration(method));
        }
        return configMap;
    }

    @Override
    public FunctionConfiguration generateConfiguration(final Method method) {
        final FunctionConfiguration config = new FunctionConfiguration();
        final List<BaseBinding> bindings = config.getBindings();

        for (final Parameter param : method.getParameters()) {
            bindings.addAll(parseAnnotations(param::getAnnotations, this::parseParameterAnnotation));
        }

        if (!method.getReturnType().equals(Void.TYPE)) {
            bindings.addAll(parseAnnotations(method::getAnnotations, this::parseMethodAnnotation));

            if (bindings.stream().anyMatch(b -> b.getType().equalsIgnoreCase(HttpTrigger.class.getSimpleName())) &&
                    bindings.stream().noneMatch(b -> b.getName().equalsIgnoreCase("$return"))) {
                bindings.add(new HttpBinding());
            }
        }

        config.setEntryPoint(method.getDeclaringClass().getCanonicalName() + "." + method.getName());
        return config;
    }

    protected List<BaseBinding> parseAnnotations(Supplier<Annotation[]> annotationProvider,
                                                 Function<Annotation, BaseBinding> annotationParser) {
        final List<BaseBinding> bindings = new ArrayList<>();

        for (final Annotation annotation : annotationProvider.get()) {
            final BaseBinding binding = annotationParser.apply(annotation);
            if (binding != null) {
                log.debug("Adding binding: " + binding.toString());
                bindings.add(binding);
            }
        }

        return bindings;
    }

    protected BaseBinding parseParameterAnnotation(final Annotation annotation) {
        if (annotation instanceof HttpTrigger) {
            return new HttpBinding((HttpTrigger) annotation);
        }
        if (annotation instanceof HttpOutput) {
            return new HttpBinding((HttpOutput) annotation);
        }
        if (annotation instanceof QueueTrigger) {
            return new QueueBinding((QueueTrigger) annotation);
        }
        if (annotation instanceof QueueOutput) {
            return new QueueBinding((QueueOutput) annotation);
        }
        if (annotation instanceof TimerTrigger) {
            return new TimerBinding((TimerTrigger) annotation);
        }
        return null;
    }

    protected BaseBinding parseMethodAnnotation(final Annotation annotation) {
        BaseBinding ret = null;
        if (annotation instanceof HttpOutput) {
            ret = new HttpBinding((HttpOutput) annotation);
        } else if (annotation instanceof QueueOutput) {
            ret = new QueueBinding((QueueOutput) annotation);
        }

        if (ret != null) {
            ret.setName("$return");
        }
        return ret;
    }
}
