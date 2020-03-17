/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.utils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class JsonUtils {
    private static final Gson GSON = new Gson();

    public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        return GSON.fromJson(json, classOfT);
    }

    public static String toJson(Object src) {
        return GSON.toJson(src);
    }

    private JsonUtils() {

    }
}
