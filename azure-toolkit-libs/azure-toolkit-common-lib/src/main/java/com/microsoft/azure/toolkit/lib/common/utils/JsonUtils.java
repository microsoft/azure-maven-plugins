/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T deepCopyWithJson(T source) {
        //noinspection unchecked
        return (T) fromJson(toJson(source), source.getClass());
    }

    public static void writeToJsonFile(File targetFile, Object json) throws IOException {
        try (Writer writer = new FileWriter(targetFile)) {
            MAPPER.writeValue(writer, json);
        }
    }

    public static <T> T readFromJsonFile(File target, Class<T> clazz) {
        try (FileInputStream fis = new FileInputStream(target);
             InputStreamReader isr = new InputStreamReader(fis)) {
            return MAPPER.readValue(isr, clazz);
        } catch (IOException e) {
            return null;
        }
    }

    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> clazz) {
        return MAPPER.readValue(json, clazz);
    }

    @SneakyThrows
    public static <T> T fromJson(String json, TypeReference<T> type) {
        return MAPPER.readValue(json, type);
    }

    @SneakyThrows
    public static String toJson(Object src) {
        return MAPPER.writeValueAsString(src);
    }
}
