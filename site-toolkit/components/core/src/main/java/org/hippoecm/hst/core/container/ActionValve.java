/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.request.HstRequestContext;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;


/**
 * ActionValveImpl
 */
public class ActionValve extends AbstractBaseOrderableValve {

    private boolean methodPostOnly;

    public void setMethodPostOnly(final boolean methodPostOnly) {
        this.methodPostOnly = methodPostOnly;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HstRequestContext requestContext = context.getRequestContext();
        HstContainerURL baseURL = requestContext.getBaseURL();
        String actionWindowReferenceNamespace = baseURL.getActionWindowReferenceNamespace();

        if (actionWindowReferenceNamespace == null) {
            // not an action request, so skip it
            context.invokeNext();
            return;
        }

        final HttpServletRequest servletRequest = context.getServletRequest();
        final HttpServletResponse servletResponse = context.getServletResponse();

        if (methodPostOnly && !HttpMethod.POST.name().equals(servletRequest.getMethod())) {
            try {
                log.info("ActionValve is only allowed to be invoked as method POST but was invoked as method {}",
                        servletRequest.getMethod());
                servletResponse.setHeader(HttpHeaders.ALLOW, HttpMethod.POST.name());
                servletResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, servletRequest.getMethod() + " Method Not Allowed");
                return;
            } catch (IOException e) {
                throw new ContainerException(e);
            }
        }

        final HstComponentWindow window = context.getRootComponentWindow();
        window.bindResponseState(servletRequest, servletResponse);
        HstRequest request = new HstRequestImpl(servletRequest, requestContext, window, HstRequest.ACTION_PHASE);
        HstResponseImpl response = new HstResponseImpl(servletRequest, servletResponse, requestContext, window, null);

        getComponentInvoker().invokeAction(context.getRequestContainerConfig(), request, response);

        final HstResponseState responseState = window.getResponseState();
        // page error handling...
        PageErrors pageErrors = getPageErrors(new HstComponentWindow [] { window }, true);
        if (pageErrors != null) {
            PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, context.getRequestContainerConfig(), window, request, response);
            String location = responseState.getRedirectLocation();
            if (handled == PageErrorHandler.Status.HANDLED_TO_STOP && location == null) {
                return;
            }
        }

        Map<String, String []> renderParameters = response.getRenderParameters();
        response.setRenderParameters(null);

        if (renderParameters == null) {
            renderParameters = Collections.emptyMap();
        }

        String referenceNamespace = window.getReferenceNamespace();

        if (getUrlFactory().isReferenceNamespaceIgnored()) {
            referenceNamespace = "";
        }

        final HstContainerURLProvider urlProvider = requestContext.getURLFactory().getContainerURLProvider();
        urlProvider.mergeParameters(baseURL, referenceNamespace, renderParameters);

        if (responseState.getErrorCode() > 0) {
            try {
                int errorCode = responseState.getErrorCode();
                String errorMessage = responseState.getErrorMessage();
                String componentClassName = window.getComponentName();

                log.debug("The action window has error status code: {} - {}", errorCode, componentClassName);

                if (errorMessage != null) {
                    servletResponse.sendError(errorCode, errorMessage);
                } else {
                    servletResponse.sendError(errorCode);
                }
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception invocation on sendError().", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception invocation on sendError().");
                }
            }
        } else {
            if (responseState.getRedirectLocation() == null) {
                try {
                    // Clear action state first
                    if (baseURL.getActionParameterMap() != null) {
                        baseURL.getActionParameterMap().clear();
                    }
                    baseURL.setActionWindowReferenceNamespace(null);

                    responseState.sendRedirect(urlProvider.toURLString(baseURL, requestContext, null));
                } catch (UnsupportedEncodingException e) {
                    throw new ContainerException(e);
                } catch (IOException e) {
                    throw new ContainerException(e);
                }
            }

            try {
                responseState.flush();

                String location = responseState.getRedirectLocation();

                if(StringUtils.isEmpty(location)) {
                    // location is the homepage (/) and there is no context path in the URL/ replace location with "/"
                    location = "/";
                }
                if (location.startsWith("?")) {
                    location = "/" + location;
                }
                if (location.startsWith("http:") || location.startsWith("https:")) {
                    servletResponse.sendRedirect(location);
                } else {
                    if (!location.startsWith("/")) {
                        throw new ContainerException("Can only redirect to a context relative path starting with a '/'.");
                    }

                        /*
                         * We will redirect to a URL containing the scheme + hostname + portnumber to avoid problems
                         * when redirecting behind a proxy by default.
                         */
                    if (isAlwaysRedirectLocationToAbsoluteUrl()) {
                        String absoluteRedirectUrl = requestContext.getVirtualHost().getBaseURL(servletRequest) + location;
                        servletResponse.sendRedirect(absoluteRedirectUrl);
                    } else {
                        servletResponse.sendRedirect(location);
                    }
                }
            } catch (IOException e) {
                log.warn("Unexpected exception during redirect to " + responseState.getRedirectLocation(), e);
            }
        }
    }

}
