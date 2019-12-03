/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.crisp.studio.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHintBuilder;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proxy servlet which reads data from client and sends it to the backend through {@link ResourceServiceBroker},
 * for developer's debugging purposes.
 */
public class ResourceServiceBrokerProxyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ResourceServiceBrokerProxyServlet.class);

    /**
     * Request parameter name to specify the operator name on {@link ResourceServiceBroker}. {@code findResources} by default.
     */
    public static final String PARAM_OP = "_op";

    /**
     * Request parameter name to specify the {@code absPath} argument on {@link ResourceServiceBroker}.
     * If not specified, the {@code absPath} argument is inferred from the request URL after the context path, the servlet path
     * and the resource space path segment.
     */
    public static final String PARAM_RESOURCE_PATH = "_path";

    /**
     * Request parameter name to specify whether enabling or disabling cache when invoking on {@link ResourceServiceBroker}.
     */
    public static final String PARAM_NOCACHE = "_nocache";

    private static final String OP_RESOLVE = "resolve";

    private static final String OP_FIND_RESOURCES = "findResources";

    private static final String OP_RESOLVE_BINARY = "resolveBinary";

    public ResourceServiceBrokerProxyServlet() {
        super();
    }

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final ResourceServiceBroker broker = getResourceServiceBroker();

        if (broker == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "ResourceServiceBroker service unavailable.");
            return;
        }

        final String opName = getOperator(request);
        final String resourceSpace = getResourceSpace(request);
        final String absPath = getResourcePath(request, resourceSpace);
        final Map<String, Object> pathVars = new HashMap<>();
        final ExchangeHint exchangeHint = buildExchangeHint(request);

        if (StringUtils.equalsIgnoreCase(opName, OP_RESOLVE_BINARY)) {
            Binary binary = null;

            try {
                binary = broker.resolveBinary(resourceSpace, absPath, pathVars, exchangeHint);
            } catch (ResourceException e) {
                final Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                if (log.isDebugEnabled()) {
                    log.warn("Exception occurred while invoking resolveBinary.", cause);
                } else {
                    log.warn("Exception occurred while invoking resolveBinary. {}", cause.toString());
                }
            }

            writeResponseStatusAndHeaders(response, exchangeHint);
            writeBinaryBodyEntity(response, binary);
        } else {
            Resource resource = null;

            try {
                if (StringUtils.equalsIgnoreCase(opName, OP_RESOLVE)) {
                    resource = broker.resolve(resourceSpace, absPath, pathVars, exchangeHint);
                } else {
                    resource = broker.findResources(resourceSpace, absPath, pathVars, exchangeHint);
                }
            } catch (ResourceException e) {
                final Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                if (log.isDebugEnabled()) {
                    log.warn("Exception occurred while finding or resolving resource(s).", cause);
                } else {
                    log.warn("Exception occurred while finding or resolving resource(s). {}", cause.toString());
                }
            }

            writeResponseStatusAndHeaders(response, exchangeHint);
            writeResourceNodeDataBodyEntity(response, resource);
        }

        response.flushBuffer();
        response.getOutputStream().close();
    }

    /**
     * Set response status code and write response headers, using the data from the backend.
     * @param response response
     * @param exchangeHint exchange hint
     * @param resourceEx resource exception
     */
    protected void writeResponseStatusAndHeaders(final HttpServletResponse response, final ExchangeHint exchangeHint) {
        final int responseStatus = exchangeHint.getResponseStatusCode();

        if (responseStatus > 0) {
            response.setStatus(responseStatus);
        }

        for (Map.Entry<String, List<String>> entry : exchangeHint.getResponseHeaders().entrySet()) {
            final String headerName = entry.getKey();

            // Skip Content-Length header; otherwise, client may try to read data in a different content length value
            // from the real serialized data out of the resource object.
            if (StringUtils.equalsIgnoreCase("Content-Length", headerName)) {
                continue;
            }

            for (String headerValue : entry.getValue()) {
                response.addHeader(headerName, headerValue);
            }
        }
    }

    /**
     * Write the response body from the backend.
     * @param response response
     * @param binary binary
     * @throws ServletException if servlet exception occurs
     * @throws IOException if IO exception occurs
     */
    protected void writeBinaryBodyEntity(final HttpServletResponse response, final Binary binary)
            throws ServletException, IOException {
        if (binary == null) {
            return;
        }

        try (InputStream input = binary.getInputStream()) {
            IOUtils.copyLarge(input, response.getOutputStream());
        } finally {
            binary.dispose();
        }
    }

    /**
     * Write resource node data as response body entity from the backend.
     * @param response response
     * @param resource resource
     * @throws ServletException if servlet exception occurs
     * @throws IOException if IO exception occurs
     */
    protected void writeResourceNodeDataBodyEntity(final HttpServletResponse response, final Resource resource)
            throws ServletException, IOException {
        if (resource == null) {
            return;
        }

        resource.dump(response.getOutputStream());
    }

    /**
     * Find and return the {@link ResourceServiceBroker} service.
     * <P>
     * This method can be overridden if the downstream project requires to find the service in a different way
     * for some reasons.
     * @return the {@link ResourceServiceBroker} service
     */
    protected ResourceServiceBroker getResourceServiceBroker() {
        return HippoServiceRegistry.getService(ResourceServiceBroker.class);
    }

    /**
     * Find and return the operatoin name on the {@link ResourceServiceBroker}. {@code findResources} by default.
     * @param request request
     * @return the operatoin name on the {@link ResourceServiceBroker}, {@code findResources} by default
     */
    protected String getOperator(final HttpServletRequest request) {
        return StringUtils.defaultIfBlank(request.getParameter(PARAM_OP), OP_FIND_RESOURCES);
    }

    /**
     * Find and return the resource space argument when invoking on the {@link ResourceServiceBroker}.
     * By default, the next URI path segment after the context path and servlet path is used as {@code resourceSpace}
     * argument.
     * @param request request
     * @return the resource space argument when invoking on the {@link ResourceServiceBroker}
     */
    protected String getResourceSpace(final HttpServletRequest request) {
        final String servletPath = request.getServletPath();
        final String contextServletPath = request.getContextPath() + ("/".equals(servletPath) ? "" : servletPath);
        final String servletRelPath = StringUtils.removeStart(request.getRequestURI(), contextServletPath);

        if (StringUtils.isEmpty(servletRelPath) || "/".equals(servletRelPath)) {
            return StringUtils.EMPTY;
        }

        final int offset = servletRelPath.indexOf("/", 1);
        return (offset != -1) ? servletRelPath.substring(1, offset) : servletRelPath.substring(1);
    }

    /**
     * Find and return the resource {@code absPath} argument when invoking on the {@link ResourceServiceBroker}.
     * <P>
     * If {@code _path} request parameter is set explicitly, then the parameter is used.
     * Otherwise, the rest URL after the context path, servlet path and {@code resourceSpace} path segment is used.
     * @param request request
     * @param resourceSpace resource space
     * @return the resource {@code absPath} argument when invoking on the {@link ResourceServiceBroker}
     */
    protected String getResourcePath(final HttpServletRequest request, final String resourceSpace) {
        final String pathParam = request.getParameter(PARAM_RESOURCE_PATH);

        if (pathParam != null) {
            return pathParam;
        }

        final String servletPath = request.getServletPath();
        final String contextServletPath = request.getContextPath() + ("/".equals(servletPath) ? "" : servletPath);
        final String servletRelPath = StringUtils.removeStart(request.getRequestURI(), contextServletPath);

        if (StringUtils.isEmpty(servletRelPath) || "/".equals(servletRelPath)) {
            return StringUtils.EMPTY;
        }

        String resourcePath = StringUtils.removeStart(servletRelPath, "/" + resourceSpace);
        final String queryString = request.getQueryString();

        if (StringUtils.isNotBlank(queryString)) {
            resourcePath += "?" + queryString;
        }

        return resourcePath;
    }

    /**
     * Build an {@link ExchangeHint} from the request, to pass to the backend.
     * @param request request
     * @return an {@link ExchangeHint} from the request, to pass to the backend
     * @throws IOException if IOException occurs
     */
    protected ExchangeHint buildExchangeHint(final HttpServletRequest request) throws IOException {
        return ExchangeHintBuilder.create()
                .methodName(request.getMethod())
                .noCache(BooleanUtils.toBoolean(request.getParameter(PARAM_NOCACHE)))
                .requestHeaders(buildRequestHeadersMap(request))
                .requestBody(buildRequestBodyEntityAsByteArray(request))
                .build();
    }

    /**
     * Build a map of request headers from the request, to pass to the backend.
     * @param request request
     * @return a map of request headers from the request, to pass to the backend
     */
    @SuppressWarnings("unchecked")
    protected Map<String, List<String>> buildRequestHeadersMap(final HttpServletRequest request) {
        final Map<String, List<String>> headersMap = new LinkedHashMap<>();

        for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements(); ) {
            final String headerName = headerNames.nextElement();
            headersMap.put(headerName, EnumerationUtils.toList(request.getHeaders(headerName)));
        }

        return headersMap;
    }

    /**
     * Build a request body entity as byte array from the request, to pass to the backend.
     * @param request request
     * @return a request body entity as byte array from the request, to pass to the backend
     * @throws IOException if IOException occurs
     */
    protected byte[] buildRequestBodyEntityAsByteArray(final HttpServletRequest request) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream(4096);

        try (InputStream input = request.getInputStream()) {
            IOUtils.copyLarge(input, output);
        }

        return output.toByteArray();
    }
}
