/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.nimbusds.jose.util.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class LocalAuthServer {
    private static transient boolean inited = false;
    private static Object initLock = new Object();
    private static String loginSuccessBodyTemplate;
    private static String loginErrorBodyTemplate;
    private final Semaphore semaphore = new Semaphore(0);
    private final Server jettyServer;

    private String code;
    private String error;
    private String errorDescription;

    public LocalAuthServer() {
        jettyServer = new Server();
        final ServerConnector connector = new ServerConnector(jettyServer);
        connector.setHost("localhost");
        jettyServer.setConnectors(new Connector[]{ connector });
        jettyServer.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                initHtmlTemplate();
                try {
                    ((Request) request).setHandled(true);
                    code = request.getParameter(Constants.CODE);
                    // handle the error response described at
                    // https://docs.microsoft.com/en-us/azure/active-directory/develop/v1-protocols-oauth-code#error-response
                    errorDescription = request.getParameter(Constants.ERROR_DESCRIPTION);
                    error = request.getParameter(Constants.ERROR);

                    final boolean isSuccess = StringUtils.isEmpty(error) && StringUtils.isNotEmpty(code);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(Constants.CONTENT_TYPE_TEXT_HTML);
                    try (final PrintWriter writer = response.getWriter()) {
                        writer.write(isSuccess ? loginSuccessBodyTemplate : String.format(loginErrorBodyTemplate, error, errorDescription));
                        writer.flush();
                    }
                    response.flushBuffer();
                } finally {
                    semaphore.release();
                }
            }

        });
    }

    public URI getURI() {
        return jettyServer.getURI();
    }

    public void start() throws IOException {
        if (jettyServer.isStarted()) {
            return;
        }
        try {
            jettyServer.start();
        } catch (Exception e) { // server.start will throw Exception
            if (e instanceof RuntimeException) {
                // avoid unnecessary exception convert
                throw (RuntimeException) e;
            }

            throw new IOException(e);
        }

    }

    public void stop() throws IOException {
        semaphore.release();
        try {
            jettyServer.stop();
        } catch (Exception e) { // server.stop will throw Exception
            if (e instanceof RuntimeException) {
                // avoid unnecessary exception convert
                throw (RuntimeException) e;
            }

            throw new IOException(e);
        }

    }

    public String waitForCode() throws InterruptedException, AzureLoginFailureException {
        if (!semaphore.tryAcquire(Constants.OAUTH_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            try {
                stop();
            } catch (IOException e) {
                // ignore
            }
        }
        if (StringUtils.isEmpty(error) && StringUtils.isNotEmpty(code)) {
            return code;
        }
        if (StringUtils.isNotEmpty(error)) {
            if (StringUtils.isNotEmpty(errorDescription)) {
                throw new AzureLoginFailureException(error + "\nReason: " + errorDescription);
            }
            throw new AzureLoginFailureException(error);
        } else {
            throw new AzureLoginFailureException("There is no error and no code.");
        }
    }

    static void initHtmlTemplate() throws IOException {
        if (inited) {
            return;
        }

        synchronized (initLock) {
            if (!inited) {
                // don't use String.replace, which will do the regular expression replacement
                loginSuccessBodyTemplate = StringUtils.replace(loadResource("success.html"), "${refresh_url}", Constants.LOGIN_LANDING_PAGE);
                loginErrorBodyTemplate = StringUtils.replace(loadResource("failure.html"), "${refresh_url}", Constants.LOGIN_LANDING_PAGE);
                inited = true;
            }
        }
    }

    static String loadResource(String resourceName) throws IOException {
        return IOUtils.readInputStreamToString(LocalAuthServer.class.getClassLoader().getResourceAsStream(resourceName), Constants.UTF8);
    }

}
