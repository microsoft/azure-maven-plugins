/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class QueryStringUtil {
    public static Map<String, String> queryToMap(String query) throws UnsupportedEncodingException {
        final Map<String, String> queryMap = new LinkedHashMap<>();
        final String[] pairs = query.split("&");
        for (final String pair : pairs) {
            final int idx = pair.indexOf("=");
            queryMap.put(decodeUtf8(pair.substring(0, idx)), decodeUtf8(pair.substring(idx + 1)));
        }
        return queryMap;
    }

    private static String decodeUtf8(String str) throws UnsupportedEncodingException {
        return URLDecoder.decode(str, "UTF-8");
    }

    private QueryStringUtil() {

    }
}
