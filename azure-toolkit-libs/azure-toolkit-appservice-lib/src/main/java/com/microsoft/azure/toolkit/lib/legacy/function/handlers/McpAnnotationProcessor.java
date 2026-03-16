/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.handlers;

import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Processor for handling MCP (Model Context Protocol) annotations in Azure Functions.
 * This class is responsible for processing McpToolTrigger, McpToolProperty, McpResourceTrigger,
 * and McpMetadata annotations and generating the appropriate binding configurations for function.json.
 * 
 * McpToolTrigger annotations define tool invocation triggers with a toolName.
 * McpToolProperty annotations define tool properties that are aggregated into toolProperties JSON.
 * McpResourceTrigger annotations define resource triggers that expose content via MCP.
 * McpMetadata annotations attach arbitrary JSON metadata to a trigger, surfaced in the MCP protocol's _meta field.
 */
public class McpAnnotationProcessor {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private McpAnnotationProcessor() {
        // Utility class - no instances allowed
    }

    /**
     * Processes all MCP-related annotations and updates the bindings accordingly.
     * This performs patching of individual bindings and generation of toolProperties in a single pass
     * for optimal performance.
     * 
     * Note: Duplicate name validation is handled at the function level by AnnotationHandlerImpl.
     * 
     * Assumes each method has at most one McpToolTrigger that receives all McpToolProperty data.
     * 
     * @param bindings the list of bindings to update
     */
    public static void processMcpAnnotations(final List<Binding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        
        final List<Map<String, Object>> allProperties = new ArrayList<>();
        final List<Binding> mcpTriggers = new ArrayList<>();
        final List<Binding> mcpMetadataBindings = new ArrayList<>();
        
        // Single pass: Process all bindings and categorize them
        for (final Binding binding : bindings) {
            final BindingEnum bindingType = binding.getBindingEnum();
            
            if (bindingType == BindingEnum.McpToolProperty) {
                processPropertyBinding(binding, allProperties);
            } else if (bindingType == BindingEnum.McpToolTrigger) {
                patchMcpToolTrigger(binding);
                mcpTriggers.add(binding);
            } else if (bindingType == BindingEnum.McpResourceTrigger) {
                patchMcpResourceTrigger(binding);
                mcpTriggers.add(binding);
            } else if (bindingType == BindingEnum.McpMetadata) {
                mcpMetadataBindings.add(binding);
            }
        }
        
        // Apply toolProperties to tool triggers (only generate JSON once if needed)
        if (!allProperties.isEmpty()) {
            final String toolPropertiesJson = JsonUtils.toJson(allProperties);
            for (final Binding trigger : mcpTriggers) {
                if (trigger.getBindingEnum() == BindingEnum.McpToolTrigger) {
                    trigger.setAttribute("toolProperties", toolPropertiesJson);
                }
            }
        }

        // Apply metadata from McpMetadata bindings to their associated triggers
        applyMetadataToTriggers(mcpMetadataBindings, mcpTriggers);

        // Remove McpMetadata bindings — they are not real bindings and should not
        // appear in function.json as separate entries
        bindings.removeAll(mcpMetadataBindings);
    }

    /**
     * Extracts the 'name' attribute from an McpToolTrigger binding and sets it as 'toolName' 
     * on the binding for function.json generation.
     * 
     * @param binding the binding to update
     */
    private static void patchMcpToolTrigger(final Binding binding) {
        final String name = (String) binding.getAttribute("name");
        if (StringUtils.isNotEmpty(name)) {
            binding.setAttribute("toolName", name);
        }
    }

    /**
     * Patches the McpResourceTrigger binding for function.json generation.
     * The 'name' attribute from the Java annotation is the binding parameter name,
     * not the resource name. The 'resourceName' and 'uri' attributes are already
     * set directly from the annotation properties.
     * 
     * @param binding the binding to update
     */
    private static void patchMcpResourceTrigger(final Binding binding) {
        // No patching needed for McpResourceTrigger — unlike McpToolTrigger where
        // 'name' maps to 'toolName', the McpResourceTrigger annotation has explicit
        // 'resourceName' and 'uri' properties that are already correctly named.
    }

    /**
     * Extracts the 'name' attribute from an McpToolProperty binding and sets it as 'propertyName' 
     * on the binding. Note: Does NOT set 'toolName' on property bindings.
     * 
     * @param binding the binding to update
     */
    private static void patchMcpToolProperty(final Binding binding) {
        final String name = (String) binding.getAttribute("name");
        if (StringUtils.isNotEmpty(name)) {
            binding.setAttribute("propertyName", name);
        }
    }

    /**
     * Processes a single McpToolProperty binding: patches it and adds its attributes 
     * to the properties collection.
     * 
     * @param binding the property binding to process
     * @param allProperties the collection to add processed attributes to
     */
    private static void processPropertyBinding(final Binding binding, 
                                               final List<Map<String, Object>> allProperties) {
        patchMcpToolProperty(binding);
        
        // Create filtered attributes map (excluding 'name' for toolProperties)
        final Map<String, Object> propertyAttributes = createFilteredAttributesMap(binding);
        allProperties.add(propertyAttributes);
    }

    /**
     * Creates a filtered map of binding attributes, excluding the 'name' attribute.
     * 
     * @param binding the binding to extract attributes from
     * @return a new map with all attributes except 'name'
     */
    private static Map<String, Object> createFilteredAttributesMap(final Binding binding) {
        final Map<String, Object> propertyAttributes = new HashMap<>();
        final Map<String, Object> bindingAttributes = binding.getBindingAttributes();
        
        for (final Map.Entry<String, Object> entry : bindingAttributes.entrySet()) {
            if (!"name".equals(entry.getKey())) {
                propertyAttributes.put(entry.getKey(), entry.getValue());
            }
        }
        
        return propertyAttributes;
    }

    /**
     * Applies metadata from McpMetadata bindings to their associated trigger bindings.
     * Each McpMetadata binding's {@code json} attribute is set as the {@code metadata}
     * property on the trigger binding that shares the same parameter {@code name}.
     * If only one trigger exists, all metadata is applied to it regardless of name matching.
     *
     * @param metadataBindings the list of McpMetadata bindings to process
     * @param triggers the list of MCP trigger bindings to apply metadata to
     */
    private static void applyMetadataToTriggers(final List<Binding> metadataBindings,
                                                 final List<Binding> triggers) {
        if (metadataBindings.isEmpty() || triggers.isEmpty()) {
            return;
        }

        for (final Binding metadata : metadataBindings) {
            final String json = (String) metadata.getAttribute("json");
            if (StringUtils.isEmpty(json)) {
                continue;
            }

            final String metadataName = metadata.getName();

            if (triggers.size() == 1) {
                // Single trigger: apply metadata directly
                triggers.get(0).setAttribute("metadata", json);
            } else {
                // Multiple triggers: match by parameter name
                for (final Binding trigger : triggers) {
                    if (StringUtils.equals(trigger.getName(), metadataName)) {
                        trigger.setAttribute("metadata", json);
                        break;
                    }
                }
            }
        }
    }

    // Fully-qualified class names of types that require useResultSchema=true in function.json.
    // For MCP SDK types, we check if the return type implements the Content interface rather
    // than listing every concrete type — this automatically covers TextContent, ImageContent,
    // AudioContent, ResourceLink, EmbeddedResource, and any future Content implementations.
    private static final Set<String> RICH_RESULT_TYPE_FQCNS = Set.of(
        "com.microsoft.azure.functions.mcp.McpToolResult",
        "io.modelcontextprotocol.spec.McpSchema.CallToolResult"
    );

    // The sealed Content interface that all MCP SDK content types implement.
    private static final String MCP_CONTENT_INTERFACE_FQCN = "io.modelcontextprotocol.spec.McpSchema.Content";

    private static final String MCP_CONTENT_ANNOTATION_FQCN = "com.microsoft.azure.functions.mcp.McpContent";

    /**
     * Inspects the function method's return type and sets {@code useResultSchema=true}
     * on the McpToolTrigger binding when the return type requires middleware wrapping.
     *
     * <p>This flag tells the host extension to use {@code ToolReturnValueBinder} (which
     * understands the {@code McpToolResult} envelope) instead of {@code SimpleToolReturnValueBinder}
     * (which just calls {@code .toString()}).</p>
     *
     * <p>The flag is only set when:</p>
     * <ul>
     *   <li>There is an McpToolTrigger binding</li>
     *   <li>The function has no output bindings</li>
     *   <li>The return type is a known rich result type or annotated with {@code @McpContent}</li>
     * </ul>
     *
     * @param method   the function method to inspect
     * @param bindings the list of bindings for this function
     */
    public static void setUseResultSchemaIfNeeded(final Method method, final List<Binding> bindings) {
        // Find the MCP tool trigger binding
        final Optional<Binding> mcpToolTrigger = bindings.stream()
                .filter(b -> b.getBindingEnum() == BindingEnum.McpToolTrigger)
                .findFirst();

        if (mcpToolTrigger.isEmpty()) {
            return;
        }

        // Don't set useResultSchema when there are output bindings
        final boolean hasOutputBindings = bindings.stream()
                .anyMatch(b -> b.getBindingEnum().getDirection() == BindingEnum.Direction.OUT);
        if (hasOutputBindings) {
            return;
        }

        final Class<?> returnType = method.getReturnType();
        if (returnType.equals(Void.TYPE)) {
            return;
        }

        if (needsResultSchema(returnType, method.getGenericReturnType())) {
            mcpToolTrigger.get().setAttribute("useResultSchema", true);
        }
    }

    /**
     * Determines whether the given return type requires {@code useResultSchema=true}.
     *
     * @param returnType        the raw return type class
     * @param genericReturnType the generic return type (for inspecting List type arguments)
     * @return true if the return type needs middleware wrapping
     */
    private static boolean needsResultSchema(final Class<?> returnType, final Type genericReturnType) {
        // Check if return type is a known rich result type (by FQCN)
        final String fqcn = returnType.getCanonicalName();
        if (fqcn != null && RICH_RESULT_TYPE_FQCNS.contains(fqcn)) {
            return true;
        }

        // Check if return type implements MCP SDK Content interface
        if (implementsInterface(returnType, MCP_CONTENT_INTERFACE_FQCN)) {
            return true;
        }

        // Check if return type is annotated with @McpContent
        if (hasAnnotationByFqcn(returnType, MCP_CONTENT_ANNOTATION_FQCN)) {
            return true;
        }

        // Check if return type is a subclass of a known rich result type
        if (isSubclassOfRichType(returnType)) {
            return true;
        }

        // Check List<Content> or List<? extends Content> via generic return type
        if (List.class.isAssignableFrom(returnType) && genericReturnType instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) genericReturnType;
            final Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?>) {
                final Class<?> elemClass = (Class<?>) typeArgs[0];
                final String elemFqcn = elemClass.getCanonicalName();
                if (elemFqcn != null && RICH_RESULT_TYPE_FQCNS.contains(elemFqcn)) {
                    return true;
                }
                if (implementsInterface(elemClass, MCP_CONTENT_INTERFACE_FQCN)) {
                    return true;
                }
                if (isSubclassOfRichType(elemClass)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks whether a class has an annotation with the given fully-qualified class name.
     * Uses FQCN string comparison to avoid requiring the annotation class on the plugin's classpath.
     */
    private static boolean hasAnnotationByFqcn(final Class<?> clazz, final String annotationFqcn) {
        for (final Annotation a : clazz.getAnnotations()) {
            if (annotationFqcn.equals(a.annotationType().getCanonicalName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a class implements or extends any known rich result type by walking
     * both the superclass chain and the implemented interfaces.
     */
    private static boolean isSubclassOfRichType(final Class<?> clazz) {
        // Check superclass chain
        Class<?> current = clazz.getSuperclass();
        while (current != null && !current.equals(Object.class)) {
            final String superFqcn = current.getCanonicalName();
            if (superFqcn != null && RICH_RESULT_TYPE_FQCNS.contains(superFqcn)) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    /**
     * Checks whether a class is, or implements (directly or transitively), an interface
     * with the given fully-qualified class name.
     */
    private static boolean implementsInterface(final Class<?> clazz, final String interfaceFqcn) {
        // Check if the class itself is the interface
        if (interfaceFqcn.equals(clazz.getCanonicalName())) {
            return true;
        }
        // Check interfaces on this class and all superclasses
        Class<?> current = clazz;
        while (current != null) {
            for (final Class<?> iface : current.getInterfaces()) {
                if (interfaceFqcn.equals(iface.getCanonicalName())) {
                    return true;
                }
                // Check super-interfaces recursively
                if (implementsInterface(iface, interfaceFqcn)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
