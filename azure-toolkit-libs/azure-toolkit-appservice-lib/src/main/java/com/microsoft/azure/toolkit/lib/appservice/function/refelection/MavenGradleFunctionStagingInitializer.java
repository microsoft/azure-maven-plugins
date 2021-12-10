/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.function.refelection;

import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionStagingInitializer;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionMethod;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionProject;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandlerImpl;
import lombok.SneakyThrows;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MavenGradleFunctionStagingInitializer extends FunctionStagingInitializer {
    private static final Logger log = LoggerFactory.getLogger(MavenGradleFunctionStagingInitializer.class);

    public MavenGradleFunctionStagingInitializer() {
        super(MavenGradleFunctionStagingInitializer::findAnnotatedMethods, MavenGradleFunctionStagingInitializer::installExtension);
    }

    public static List<FunctionMethod> findAnnotatedMethods(FunctionProject project) {
        Set<Method> methods;
        try {
            try {
                log.debug("ClassPath to resolve: " + getTargetClassUrl(project));
                final List<URL> dependencyWithTargetClass = getDependencyArtifactUrls(project);
                dependencyWithTargetClass.add(getTargetClassUrl(project));
                methods = findFunctions(dependencyWithTargetClass);
            } catch (NoClassDefFoundError e) {
                // fallback to reflect through artifact url, for shaded project(fat jar)
                log.debug("ClassPath to resolve: " + getArtifactUrl(project));
                methods = findFunctions(Collections.singletonList(getArtifactUrl(project)));
            }
            return methods.stream().map(RefelectionFunctionAdaptor::create).collect(Collectors.toList());
        } catch (MalformedURLException e) {
            throw new AzureToolkitRuntimeException("Invalid URL when resolving functions in class path:" + e.getMessage(), e);
        }
    }

    @SneakyThrows
    private static void installExtension(FunctionProject project) {
        final CommandHandler commandHandler = new CommandHandlerImpl();
        final FunctionCoreToolsHandler functionCoreToolsHandler = getFunctionCoreToolsHandler(commandHandler);
        functionCoreToolsHandler.installExtension(project.getStagingFolder(),
                project.getBaseDirectory());
    }

    private static FunctionCoreToolsHandler getFunctionCoreToolsHandler(final CommandHandler commandHandler) {
        return new FunctionCoreToolsHandlerImpl(commandHandler);
    }

    private static URL getTargetClassUrl(FunctionProject project) throws MalformedURLException {
        return project.getClassesOutputDirectory().toURI().toURL();
    }

    /**
     * @return URLs for the classpath with compile scope needed jars
     */
    private static List<URL> getDependencyArtifactUrls(FunctionProject project) {
        final List<URL> urlList = new ArrayList<>();
        project.getDependencies().forEach(file -> {
            try {
                urlList.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                log.debug("Failed to get URL for file: " + file);
            }
        });
        return urlList;
    }

    private static Set<Method> findFunctions(final List<URL> urls) {
        return new Reflections(
                new ConfigurationBuilder()
                        .addUrls(urls)
                        .setScanners(Scanners.MethodsAnnotated)
                        .addClassLoaders(getClassLoader(urls)))
                .getMethodsAnnotatedWith(FunctionName.class);
    }

    private static ClassLoader getClassLoader(final List<URL> urlList) {
        final URL[] urlArray = urlList.toArray(new URL[0]);
        return new URLClassLoader(urlArray, MavenGradleFunctionStagingInitializer.class.getClassLoader());
    }

    private static URL getArtifactUrl(FunctionProject project) throws MalformedURLException {
        return project.getArtifactFile().toURI().toURL();
    }
}
