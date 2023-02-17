/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingsTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplates;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FunctionUtils {
    private static final String LOAD_TEMPLATES_FAIL = "Failed to load all function templates.";
    private static final String LOAD_BINDING_TEMPLATES_FAIL = "Failed to load function binding template.";
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+|\\*)");

    public static FunctionExtensionVersion parseFunctionExtensionVersion(String version) {
        return Arrays.stream(FunctionExtensionVersion.values())
                .filter(versionEnum -> StringUtils.equalsIgnoreCase(versionEnum.getVersion(), version) ||
                        StringUtils.equalsIgnoreCase(String.valueOf(versionEnum.getValue()), version))
                .findFirst()
                .orElse(FunctionExtensionVersion.UNKNOWN);
    }

    public static FunctionExtensionVersion parseFunctionExtensionVersionFromHostJson(String version) {
        final Matcher matcher = VERSION_PATTERN.matcher(version);
        return matcher.find() ? parseFunctionExtensionVersion(matcher.group(1)) : null;
    }

    @Nullable
    public static BindingTemplate loadBindingTemplate(String type) {
        return loadBindingTemplate(new BindingConfiguration(type, "in"));
    }

    @Nullable
    public static BindingTemplate loadBindingTemplate(@Nonnull final BindingConfiguration conf) {
        return Optional.ofNullable(loadBindingsTemplate())
                .map(template -> template.getBindingTemplate(conf))
                .orElse(null);
    }

    @Nullable
    @Cacheable(value = "function-bindings")
    public static BindingsTemplate loadBindingsTemplate() {
        try (final InputStream is = FunctionUtils.class.getResourceAsStream("/bindings.json")) {
            final String bindingsJsonStr = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            return new ObjectMapper().readValue(bindingsJsonStr, BindingsTemplate.class);
        } catch (IOException e) {
            AzureMessager.getMessager().warning(LOAD_BINDING_TEMPLATES_FAIL);
            // Add task should work without Binding Template, just return null if binding load fail
            return null;
        }
    }

    @Nonnull
    @Preload
    @Cacheable(value = "function-templates")
    public static List<FunctionTemplate> loadAllFunctionTemplates() {
        try (final InputStream is = FunctionUtils.class.getResourceAsStream("/templates.json")) {
            final String templatesJsonStr = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            final FunctionTemplates templates = new ObjectMapper().readValue(templatesJsonStr, FunctionTemplates.class);
            final BindingsTemplate bindings = loadBindingsTemplate();
            final List<FunctionTemplate> result = Optional.ofNullable(templates)
                    .map(FunctionTemplates::getTemplates).orElse(Collections.emptyList());
            result.forEach(template -> Optional.ofNullable(bindings).map(binding ->
                    binding.getBindingTemplate(template.getBindingConfiguration())).ifPresent(template::setBinding));
            return result;
        } catch (Exception e) {
            throw new AzureToolkitRuntimeException(LOAD_TEMPLATES_FAIL, e);
        }
    }
}
