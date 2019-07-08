/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth.oauth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("restriction")
public class WebAppServer {
    private static final String MAVEN_PLUGIN_README_URL = 
            "https://docs.microsoft.com/en-us/java/api/overview/azure/maven/azure-webapp-maven-plugin/readme/";
    private static final String SUCCESS_BODY = "\n" +
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\" />\n" +
            "    <meta http-equiv=\"refresh\" content=\"10;url=" + MAVEN_PLUGIN_README_URL +
            "/\">\n" +
            "    <title>Login successfully</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h4>You have logged into Microsoft Azure!</h4>\n" +
            "    <p>You can close this window, or we will redirect you to the " +
            "<a href=\"" + MAVEN_PLUGIN_README_URL + "\">" +
            "Azure Maven Plugin documents</a> " +
            "in 10 seconds.</p>\n" +
            "</body>\n" +
            "</html>";

    private static final String ERROR_BODY = "\n" +
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\" />\n" +
            "    <meta http-equiv=\"refresh\" content=\"10;url=" + MAVEN_PLUGIN_README_URL + "/\">\n" +
            "    <title>Login failed</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h4>%s</h4>\n" +
            "    <p>%s</p>\n" +
            "</body>\n" +
            "</html>";

    HttpServer server;
    String code;
    String error;
    String errorDescription;
    boolean finish;

    public WebAppServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", new HttpHandler() {

            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                try {
                    final Map<String, String> attributeMap = splitQuery(httpExchange.getRequestURI());
                    WebAppServer.this.code = attributeMap.get("code");
                    WebAppServer.this.error = attributeMap.get("error");
                    WebAppServer.this.errorDescription = attributeMap.get("error_description");

                    final boolean isSuccess = StringUtils.isEmpty(error) && StringUtils.isNotEmpty(code);
                    final OutputStreamWriter osw = new OutputStreamWriter(httpExchange.getResponseBody());
                    if (isSuccess) {
                        httpExchange.sendResponseHeaders(200, SUCCESS_BODY.length());
                        osw.write(SUCCESS_BODY);
                        osw.flush();
                    } else {
                        final String response = String.format(ERROR_BODY, error, errorDescription);
                        httpExchange.sendResponseHeaders(200, response.length());
                        osw.write(response);
                        osw.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.stop(0);
                    finish = true;
                }
            }

            private Map<String, String> splitQuery(URI url) throws UnsupportedEncodingException {
                final Map<String, String> queryMap = new LinkedHashMap<String, String>();
                final String[] pairs = url.getQuery().split("&");
                for (final String pair : pairs) {
                    final int idx = pair.indexOf("=");
                    queryMap.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
                return queryMap;
            }

        });
        server.setExecutor(null); // creates a default executor
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public String getResult() {
        try {
            while (!finish) {
                Thread.sleep(50);
            }
            if (StringUtils.isEmpty(error) && StringUtils.isNotEmpty(code)) {
                return code;
            } else {
                throw new RuntimeException(error + ":" + errorDescription);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
