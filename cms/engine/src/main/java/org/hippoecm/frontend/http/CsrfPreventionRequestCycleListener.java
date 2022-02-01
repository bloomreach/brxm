/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original fork of wicket's  6.21.0 CsrfPreventionRequestCycleListener,
 * see https://github.com/apache/wicket/blob/build/wicket-6.21.0/wicket-core/src/main/java/org/apache/wicket/protocol/http/CsrfPreventionRequestCycleListener.java.
 * This class has been forked and modified to support a CsrfPreventionRequestCycleListener working behind proxies like httpd
 */
package org.hippoecm.frontend.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.util.lang.Checks;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hippoecm.frontend.util.RequestUtils.getFarthestRequestScheme;

/**
 * Prevents CSRF attacks on Wicket components by checking the {@code Origin} HTTP header for cross
 * domain requests. By default only checks requests that try to perform an action on a component,
 * such as a form submit, or link click.
 * <p>
 * <h3>Installation</h3>
 * <p>
 * You can enable this CSRF prevention filter by adding it to the request cycle listeners in your
 * {@link WebApplication#init()}  application's init method:
 *
 * <pre>
 * &#064;Override
 * protected void init()
 * {
 * 	// ...
 * 	getRequestCycleListeners().add(new CsrfPreventionRequestCycleListener());
 * 	// ...
 * }
 * </pre>
 * <p>
 * <h3>Configuration</h3>
 * <p>
 * A missing {@code Origin} HTTP header is (by default) aborted for action (POST, DELETE, PUT, PATCH) requests.
 * You can {@link #setNoOriginAction(CsrfAction) configure the specific action} to a
 * different value, suppressing or allowing the request when the {@code Origin} HTTP header is
 * missing.
 * <p>
 * When the {@code Origin} HTTP header doesn't match the requested URL this listener
 * will by default throw a HTTP error ( {@code 400 BAD REQUEST}) for action requests and abort the request. You can
 * {@link #setConflictingOriginAction(CsrfAction) configure} this specific action.
 * <p>
 * When you want to accept certain cross domain request from a range of hosts, you can
 * {@link #addAcceptedOrigin(String) whitelist those domains}.
 * <p>
 * You can override the default actions that are performed by overriding the event handlers for
 * them:
 * <ul>
 * <li>{@link #onWhitelisted(HttpServletRequest, String, IRequestablePage)} when an origin was
 * whitelisted</li>
 * <li>{@link #onMatchingOrigin(HttpServletRequest, String, IRequestablePage)} when an origin was
 * matching</li>
 * <li>{@link #onAborted(HttpServletRequest, String, IRequestablePage)} when an origin was in
 * conflict and the request should be aborted</li>
 * <li>{@link #onAllowed(HttpServletRequest, String, IRequestablePage)} when an origin was in
 * conflict and the request should be allowed</li>
 * <li>{@link #onSuppressed(HttpServletRequest, String, IRequestablePage)} when an origin was in
 * conflict and the request should be suppressed</li>
 * </ul>
 */
public class CsrfPreventionRequestCycleListener implements IRequestCycleListener {

    private static final Logger log = LoggerFactory.getLogger(CsrfPreventionRequestCycleListener.class);

    /**
     * The action to perform when a missing or conflicting Origin header is detected.
     */
    public enum CsrfAction {
        /** Aborts the request and throws an exception when a CSRF request is detected. */
        ABORT {
            @Override
            public String toString()
            {
                return "aborted";
            }
        },

        /**
         * Ignores the action of a CSRF request, and just renders the page it was targeted against.
         */
        SUPPRESS {
            @Override
            public String toString()
            {
                return "suppressed";
            }
        },

        /** Detects a CSRF request, logs it and allows the request to continue. */
        ALLOW {
            @Override
            public String toString()
            {
                return "allowed";
            }
        },
    }

    /**
     * Action to perform when no Origin header is present in the request. Default is ABORT
     */
    private CsrfAction noOriginAction = CsrfAction.ABORT;

    /**
     * Action to perform when a conflicing Origin header is found.
     */
    private CsrfAction conflictingOriginAction = CsrfAction.ABORT;

    /**
     * The error code to report when the action to take for a CSRF request is
     * {@link CsrfAction#ABORT}. Default {@code 400 BAD REQUEST}.
     */
    private int errorCode = javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

    /**
     * The error message to report when the action to take for a CSRF request is {@code ERROR}.
     * Default {@code "Origin does not correspond to request"}.
     */
    private String errorMessage = "Origin and Referer header are null or do not correspond to request origin.";

    /**
     * A white list of accepted origins (host names/domain names) presented as
     * &lt;domainname&gt;.&lt;TLD&gt;. The domain part can contain subdomains.
     */
    private Collection<String> acceptedOrigins = new ArrayList<>();

    /**
     * Sets the action when no Origin header is present in the request. Default {@code ALLOW}.
     *
     * @param action
     *            the alternate action
     *
     * @return this (for chaining)
     */
    public CsrfPreventionRequestCycleListener setNoOriginAction(CsrfAction action)
    {
        this.noOriginAction = action;
        return this;
    }

    /**
     * Sets the action when a conflicting Origin header is detected. Default is {@code ERROR}.
     *
     * @param action
     *            the alternate action
     *
     * @return this
     */
    public CsrfPreventionRequestCycleListener setConflictingOriginAction(CsrfAction action)
    {
        this.conflictingOriginAction = action;
        return this;
    }

    /**
     * Modifies the HTTP error code in the exception when a conflicting Origin header is detected.
     *
     * @param errorCode
     *            the alternate HTTP error code, default {@code 400 BAD REQUEST}
     *
     * @return this
     */
    public CsrfPreventionRequestCycleListener setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
        return this;
    }

    /**
     * Modifies the HTTP message in the exception when a conflicting Origin header is detected.
     *
     * @param errorMessage
     *            the alternate message
     *
     * @return this
     */
    public CsrfPreventionRequestCycleListener setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * Adds an origin (host name/domain name) to the white list. An origin is in the form of
     * &lt;domainname&gt;.&lt;TLD&gt;, and can contain a subdomain. Every Origin header that matches
     * a domain from the whitelist is accepted and not checked any further for CSRF issues.
     *
     * E.g. when {@code example.com} is in the white list, this allows requests from (i.e. with an
     * {@code Origin:} header containing) {@code example.com} and {@code blabla.example.com} but
     * rejects requests from {@code blablaexample.com} and {@code example2.com}.
     *
     * @param acceptedOrigin
     *            the acceptable origin
     * @return this
     */
    public CsrfPreventionRequestCycleListener addAcceptedOrigin(String acceptedOrigin)
    {
        Checks.notNull("acceptedOrigin", acceptedOrigin);

        // strip any leading dot characters
        final int len = acceptedOrigin.length();
        int i = 0;
        while (i < len && acceptedOrigin.charAt(i) == '.')
        {
            i++;
        }
        acceptedOrigins.add(acceptedOrigin.substring(i));
        return this;
    }

    @Override
    public void onBeginRequest(RequestCycle cycle)
    {
        if (log.isDebugEnabled())
        {
            HttpServletRequest containerRequest = (HttpServletRequest)cycle.getRequest()
                    .getContainerRequest();
            String origin = containerRequest.getHeader("Origin");
            log.debug("Request header Origin: {}", origin);
        }
    }

    @Override
    public void onRequestHandlerResolved(RequestCycle cycle, IRequestHandler handler)
    {

        HttpServletRequest containerRequest = (HttpServletRequest)cycle.getRequest()
                .getContainerRequest();

        // CsrfPreventionRequestCycleListener#abortHandler triggers RequestCycle.execute() to execute this code again
        // but this time with a error handler which is not an instance of IPageRequestHandler
        if (isActionRequest(containerRequest) && handler instanceof IPageRequestHandler) {
            IPageRequestHandler prh = (IPageRequestHandler)handler;
            IRequestablePage targetedPage = prh.getPage();
            String origin = containerRequest.getHeader("Origin");

            // if so check the Origin HTTP header and if the Origin header is missing, check the referer
            if (origin == null) {
                log.debug("'Origin' header missing, use 'Referer' header as recommended fallback by OWASP");
                final String referer = containerRequest.getHeader("Referer");
                if (referer != null) {
                    final String scheme = substringBefore(referer, "://");
                    // host possibly including port
                    final String host = substringBefore(substringAfter(referer,scheme + "://"), "/");
                    origin = scheme + "://" + host;
                }
            }
            checkOrigin(containerRequest, origin, targetedPage);

        } else {
            log.trace("Request is not an action request so no Origin/Referer header check required");
        }
    }

    private boolean isActionRequest(final HttpServletRequest containerRequest) {
        final String method = containerRequest.getMethod();

        if (HttpMethod.GET.equals(method)) {
            return false;
        }
        if (HttpMethod.POST.equals(method)) {
            return true;
        }

        if (HttpMethod.PUT.equals(method)) {
            return true;
        }
        if (HttpMethod.DELETE.equals(method)) {
            return true;
        }
        if (HttpMethod.PATCH.equals(method)) {
            return true;
        }
        // OPTIONS, HEAD
        return false;
    }

    /**
     * Performs the check of the Origin, where the Origin can be from the {@code Origin} header
     * or if the {@code Origin} is missing the {@code Referer} header.
     *
     * @param request
     *            the current container request
     * @param origin
     *            the {@code Origin} header
     * @param page
     *            the page that is the target of the request
     */
    private void checkOrigin(HttpServletRequest request, String origin, IRequestablePage page)
    {
        if (origin == null || origin.isEmpty())
        {
            log.debug("'Origin' nor 'Referer' header present in request, {}", noOriginAction);
            switch (noOriginAction)
            {
                case ALLOW :
                    allowHandler(request, origin, page);
                    break;
                case SUPPRESS :
                    suppressHandler(request, origin, page);
                    break;
                case ABORT :
                    abortHandler(request, origin, page);
                    break;
            }
            return;
        }
        origin = origin.toLowerCase();

        // if the origin is a know and trusted origin, don't check any further but allow the request
        if (isWhitelistedOrigin(origin))
        {
            whitelistedHandler(request, origin, page);
            return;
        }

        // check if the origin HTTP header matches the request URI
        if (!isLocalOrigin(request, origin))
        {
            log.info("'Origin' or 'Referer' header '{}' conflicts with request host '{}' : {}",
                    origin, getLocationHeaderOrigin(request),  conflictingOriginAction);
            switch (conflictingOriginAction)
            {
                case ALLOW :
                    allowHandler(request, origin, page);
                    break;
                case SUPPRESS :
                    suppressHandler(request, origin, page);
                    break;
                case ABORT :
                    abortHandler(request, origin, page);
                    break;
            }
        }
        else
        {
            matchingOrigin(request, origin, page);
        }
    }

    /**
     * Checks whether the domain part of the {@code Origin} HTTP header is whitelisted.
     *
     * @param origin
     *            the {@code Origin} HTTP header
     * @return {@code true} when the origin domain was whitelisted
     */
    private boolean isWhitelistedOrigin(final String origin)
    {
        try
        {
            final URI originUri = new URI(origin);
            final String originHost = originUri.getHost();
            if (Strings.isEmpty(originHost))
                return false;
            for (String whitelistedOrigin : acceptedOrigins)
            {
                if (originHost.equalsIgnoreCase(whitelistedOrigin) ||
                        originHost.endsWith("." + whitelistedOrigin))
                {
                    log.trace("Origin or Referer {} matched whitelisted origin {}, request accepted", origin,
                            whitelistedOrigin);
                    return true;
                }
            }
        }
        catch (URISyntaxException e)
        {
            log.debug("Origin: {} not parseable as an URI. Whitelisted-origin check skipped.",
                    origin);
        }

        return false;
    }

    /**
     * Checks whether the {@code Origin} HTTP header of the request matches where the request came
     * from.
     *
     * @param containerRequest
     *            the current container request
     * @param origin
     *            the contents of the {@code Origin} HTTP header
     * @return {@code true} when the origin of the request matches the {@code Origin} HTTP header
     */
    private boolean isLocalOrigin(HttpServletRequest containerRequest, String origin)
    {
        // Make comparable strings from Origin and Location
        if (origin == null)
            return false;

        String request = getLocationHeaderOrigin(containerRequest);
        if (request == null)
            return false;

        final boolean isLocal = origin.equalsIgnoreCase(request);
        if (!isLocal && origin.startsWith("https:") && !request.startsWith("https:")) {
            log.warn("Origin or Referer starts with https: but request starts with http:. If you are running behind a proxy, make " +
                    "sure to set 'X-Forwarded-Proto: https' in the proxy");
        }
        return isLocal;
    }

    /**
     * Creates a RFC-6454 comparable origin from the {@code request} requested resource.
     *
     * @param request
     *            the incoming request
     * @return only the scheme://host[:port] part, or {@code null} when the origin string is not
     *         compliant
     */
    private String getLocationHeaderOrigin(HttpServletRequest request)
    {

        String host = request.getHeader("X-Forwarded-Host");
        if (host != null) {
            String[] hosts = host.split(",");
            final String location = getFarthestRequestScheme(request) + "://" + hosts[0];
            log.debug("X-Forwarded-Host header found. Return location '{}'", location);
            return location;
        }

        host = request.getHeader("Host");
        if (host != null && !"".equals(host)) {
            final String location = getFarthestRequestScheme(request) + "://" + host;
            log.debug("Host header found. Return location '{}'", location);
            return location;
        }

        // Build scheme://host:port from request
        StringBuilder target = new StringBuilder();
        String scheme = request.getScheme();
        if (scheme == null)
        {
            return null;
        }
        else
        {
            scheme = scheme.toLowerCase(Locale.ENGLISH);
        }
        target.append(scheme);
        target.append("://");

        host = request.getServerName();
        if (host == null)
        {
            return null;
        }
        target.append(host);

        return target.toString();
    }

    /**
     * Handles the case where an origin is in the whitelist. Default action is to allow the
     * whitelisted origin.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header
     * @param page
     *            the page that is targeted with this request
     */
    private void whitelistedHandler(HttpServletRequest request, String origin,
                                    IRequestablePage page)
    {
        onWhitelisted(request, origin, page);
        if (log.isDebugEnabled())
        {
            log.debug("CSRF Origin or Referer {} was whitelisted, allowed for page {}", origin,
                    page.getClass().getName());
        }
    }

    /**
     * Called when the origin was available in the whitelist. Override this method to implement your
     * own custom action.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header
     * @param page
     *            the page that is targeted with this request
     */
    protected void onWhitelisted(HttpServletRequest request, String origin, IRequestablePage page)
    {
    }

    /**
     * Handles the case where an origin was checked and matched the request origin. Default action
     * is to allow the whitelisted origin.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header
     * @param page
     *            the page that is targeted with this request
     */
    private void matchingOrigin(HttpServletRequest request, String origin, IRequestablePage page)
    {
        onMatchingOrigin(request, origin, page);
        if (log.isDebugEnabled())
        {
            log.debug("CSRF Origin {} matched requested resource, allowed for page {}", origin,
                    page.getClass().getName());
        }
    }

    /**
     * Called when the origin HTTP header matched the request. Override this method to implement
     * your own custom action.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header
     * @param page
     *            the page that is targeted with this request
     */
    protected void onMatchingOrigin(HttpServletRequest request, String origin,
                                    IRequestablePage page)
    {
    }

    /**
     * Handles the case where an Origin HTTP header was not present or did not match the request
     * origin, and the corresponding action ({@link #noOriginAction} or
     * {@link #conflictingOriginAction}) is set to {@code ALLOW}.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header, may be {@code null} or empty
     * @param page
     *            the page that is targeted with this request
     */
    private void allowHandler(HttpServletRequest request, String origin, IRequestablePage page)
    {
        onAllowed(request, origin, page);
        log.info("Possible CSRF attack, client request location: {}, Origin: {}, action: allowed",
                getLocationHeaderOrigin(request), origin);
    }

    /**
     * Override this method to customize the case where an Origin HTTP header was not present or did
     * not match the request origin, and the corresponding action ({@link #noOriginAction} or
     * {@link #conflictingOriginAction}) is set to {@code ALLOW}.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header, may be {@code null} or empty
     * @param page
     *            the page that is targeted with this request
     */
    protected void onAllowed(HttpServletRequest request, String origin, IRequestablePage page)
    {
    }

    /**
     * Handles the case where an Origin HTTP header was not present or did not match the request
     * origin, and the corresponding action ({@link #noOriginAction} or
     * {@link #conflictingOriginAction}) is set to {@code SUPPRESS}.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header, may be {@code null} or empty
     * @param page
     *            the page that is targeted with this request
     */
    private void suppressHandler(HttpServletRequest request, String origin, IRequestablePage page)
    {
        onSuppressed(request, origin, page);
        log.info("Possible CSRF attack, client request location: {}, Origin: {}, action: suppressed",
                getLocationHeaderOrigin(request), origin);
        throw new RestartResponseException(page);
    }

    /**
     * Override this method to customize the case where an Origin HTTP header was not present or did
     * not match the request origin, and the corresponding action ({@link #noOriginAction} or
     * {@link #conflictingOriginAction}) is set to {@code SUPPRESSED}.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header, may be {@code null} or empty
     * @param page
     *            the page that is targeted with this request
     */
    protected void onSuppressed(HttpServletRequest request, String origin, IRequestablePage page)
    {
    }

    /**
     * Handles the case where an Origin HTTP header was not present or did not match the request
     * origin, and the corresponding action ({@link #noOriginAction} or
     * {@link #conflictingOriginAction}) is set to {@code ABORT}.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header, may be {@code null} or empty
     * @param page
     *            the page that is targeted with this request
     */
    private void abortHandler(HttpServletRequest request, String origin, IRequestablePage page)
    {
        onAborted(request, origin, page);
        log.debug("Possible CSRF attack, client request location: {}, Origin: {}, action: aborted with error {} {}",
                getLocationHeaderOrigin(request), origin, errorCode, errorMessage);
        throw new AbortWithHttpErrorCodeException(errorCode, errorMessage);
    }

    /**
     * Override this method to customize the case where an Origin HTTP header was not present or did
     * not match the request origin, and the corresponding action ({@link #noOriginAction} or
     * {@link #conflictingOriginAction}) is set to {@code ABORTED}.
     *
     * @param request
     *            the request
     * @param origin
     *            the contents of the {@code Origin} HTTP header, may be {@code null} or empty
     * @param page
     *            the page that is targeted with this request
     */
    protected void onAborted(HttpServletRequest request, String origin, IRequestablePage page)
    {
    }
}


