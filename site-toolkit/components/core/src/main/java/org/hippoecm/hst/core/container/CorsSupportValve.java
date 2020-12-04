/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.header.AccessControlAllowHeadersService;
import org.hippoecm.hst.util.HstRequestUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.PRAGMA;
import static org.springframework.http.HttpHeaders.VARY;


/**
 * <p> Note that the hst:responseheaders configurable on VirtualHost, Mount or SitemapItem already have been written to
 * the response.<br/> See HstDelegateeFilterBean#writeDefaultResponseHeaders(...)).<br/> If the headers
 * ACCESS_CONTROL_ALLOW_METHODS, ACCESS_CONTROL_ALLOW_HEADERS are already set we combine these header values. Although
 * according the http spec (see below) adding headers should be allowed, it is cleaner to merge them into one header
 * name and remove duplicates. If the header ACCESS_CONTROL_MAX_AGE is already set, we do not reset it to one day like
 * we do by default with this CorsSupportValve </p> <p> Note that if the hst:responseheaders already SETs a certain
 * header name, for example "Access-Control-Allow-Methods", if should not matter that we add the same header again,
 * possibly with overlapping values <p>
 * <pre>
 *         https://tools.ietf.org/html/rfc2616#section-4.2
 *
 *         Multiple message-header fields with the same field-name MAY be
 *         present in a message if and only if the entire field-value for that
 *        header field is defined as a comma-separated list [i.e., #(values)].
 *        It MUST be possible to combine the multiple header fields into one
 *        "field-name: field-value" pair, without changing the semantics of the
 *        message, by appending each subsequent field-value to the first, each
 *       separated by a comma. The order in which header fields with the same
 *        field-name are received is therefore significant to the
 *        interpretation of the combined field value, and thus a proxy MUST NOT
 *        change the order of these field values when a message is forwarded.
 *     </pre>
 * </p>
 */
public class CorsSupportValve implements Valve {

    private final static Logger log = LoggerFactory.getLogger(CorsSupportValve.class);
    private boolean allowCredentials;
    private boolean allowOptionsRequest;

    public final static String[] KNOWN_CORS_HEADER_NAMES = new String[]{
            ACCESS_CONTROL_MAX_AGE,
            ACCESS_CONTROL_ALLOW_HEADERS,
            ACCESS_CONTROL_ALLOW_METHODS,
            VARY,
            ACCESS_CONTROL_ALLOW_ORIGIN,
            ACCESS_CONTROL_ALLOW_CREDENTIALS
    };

    // if true, Access-Control-Allow-Origin: * gets replaced with Access-Control-Allow-Origin: {client-origin}
    private boolean replaceWildcardAllowOrigin;

    public void setAllowCredentials(final boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public void setAllowOptionsRequest(final boolean allowOptionsRequest) {
        this.allowOptionsRequest = allowOptionsRequest;
    }

    public void setReplaceWildcardAllowOrigin(final boolean replaceWildcardAllowOrigin) {
        this.replaceWildcardAllowOrigin = replaceWildcardAllowOrigin;
    }

    @Override
    public void invoke(final ValveContext context) throws ContainerException {

        final HttpServletRequest servletRequest = context.getServletRequest();
        final HttpServletResponse servletResponse = context.getServletResponse();

        if (HttpMethod.OPTIONS.matches(servletRequest.getMethod())) {

            log.debug("OPTIONS request {} will be handled by CorsSupportValve", servletRequest);

            if (!allowOptionsRequest) {
                // empty possible already set CORS headers
                for (String knownCorsHeaderName : KNOWN_CORS_HEADER_NAMES) {
                    if (servletResponse.containsHeader(knownCorsHeaderName)) {
                        // unfortunately you cannot remove a header and setting null doesn't do anything, hence empty
                        // string
                        servletResponse.setHeader(knownCorsHeaderName, EMPTY);
                    }
                }
                try {
                    servletResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method OPTIONS is not " +
                            "allowed for " + servletRequest.toString());
                    return;
                } catch (IOException e) {
                    throw new ContainerException(e);
                }
            }

            servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);

            String requestOrigin = HstRequestUtils.getOrigin(servletRequest);
            // check allowed hosts

            if (originAllowed(requestOrigin, servletResponse, context.getRequestContext().getResolvedMount().getMount())) {
                setAccessControlAllowOrigin(servletResponse, requestOrigin);
            } else {
                log.info("Request Origin '{}' is not allowed", requestOrigin);
                // possible the hst:responseheaders already did set ACCESS_CONTROL_ALLOW_ORIGIN. In that case we keep it
                // as is since explicitly set

                // request (pipeline) handling completed here.
                return;
            }

            if (allowCredentials) {
                servletResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }

            // the CorsSupportValve only allows HEAD, GET and POST, since PUT and DELETE
            // are not applicable for the pipelines that use CorsSupportValve
            Collection<String> alreadySetHeaders = servletResponse.getHeaders(ACCESS_CONTROL_ALLOW_METHODS);

            if (alreadySetHeaders != null && !alreadySetHeaders.isEmpty()) {
                servletResponse.setHeader(ACCESS_CONTROL_ALLOW_METHODS, merge(alreadySetHeaders, "HEAD", "GET", "POST"));
            } else {
                servletResponse.setHeader(ACCESS_CONTROL_ALLOW_METHODS, "HEAD, GET, POST");
            }

            // regardless of "Access-Control-Request-Headers" we return all allowed headers
            AccessControlAllowHeadersService service = HippoServiceRegistry.getService(AccessControlAllowHeadersService.class);
            if (service != null) {
                final String allowedHeadersString = service.getAllowedHeadersString();
                if (isNotBlank(allowedHeadersString)) {
                    alreadySetHeaders = servletResponse.getHeaders(ACCESS_CONTROL_ALLOW_HEADERS);
                    if (alreadySetHeaders != null) {
                        servletResponse.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, merge(alreadySetHeaders, allowedHeadersString));
                    } else {
                        servletResponse.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, allowedHeadersString);
                    }
                }
            } else {
                log.info("No AccessControlAllowHeadersService present, do not set {} headers", ACCESS_CONTROL_ALLOW_HEADERS);
            }

            // A preflight request should be cached on the client to avoid frequent preflight requests for the same
            // resource. If the cache control is already set via hst:responseheaders we do not override it
            if (servletResponse.containsHeader(ACCESS_CONTROL_MAX_AGE)) {
                log.info("Header '{}' already set to value '{}', keeping that value", ACCESS_CONTROL_MAX_AGE, servletResponse.getHeader(ACCESS_CONTROL_MAX_AGE));
            } else {
                // default cache header of 1 day
                servletResponse.setHeader(ACCESS_CONTROL_MAX_AGE, "86400");
            }

            setVaryHeaderConditionally(servletResponse, HttpHeaders.ORIGIN);
            // request (pipeline) handling completed here!
        } else {

            final String requestOrigin = HstRequestUtils.getOrigin(servletRequest);
            if (originAllowed(requestOrigin, servletResponse, context.getRequestContext().getResolvedMount().getMount())){
                setAccessControlAllowOrigin(servletResponse, requestOrigin);
            }

            if (allowCredentials) {
                servletResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }

            setVaryHeaderConditionally(servletResponse, HttpHeaders.ORIGIN);

            if (context.getRequestContext().isChannelManagerPreviewRequest() ||
                    context.getRequestContext().getResolvedMount().getMount().isPreview()) {
                // regardless whether cache-control headers are set, we override them to now allow caching for
                // preview responses of Channel Manager preview
                servletResponse.setHeader(CACHE_CONTROL, "private, max-age=0, no-store");
                servletResponse.setHeader(PRAGMA, "no-cache");
            }

            context.invokeNext();
        }
    }

    private void setVaryHeaderConditionally(final HttpServletResponse servletResponse,
                               final String value) {
        if (servletResponse.containsHeader(VARY)) {
            log.info("Header '{}' already set to value '{}', keeping that value", VARY, servletResponse.getHeader(VARY));
        } else {
            log.info("Setting header {} to 'Origin' to make sure caching proxies do not return cached results for " +
                    "different origins", VARY);
            servletResponse.setHeader(VARY, value);
        }
    }

    private boolean originAllowed(final String requestOrigin, final HttpServletResponse servletResponse, final Mount mount) {
        if (isAllowedOriginResponseHeaderWildcard(servletResponse)) {
            return true;
        }

        if (servletResponse.containsHeader(ACCESS_CONTROL_ALLOW_ORIGIN)) {
            // regardless whether mount.getVirtualHost().getAllowedOrigins() allows the origin, when an explicit
            // hst:responseheader for a certain host is set, we do not look at whitelisted origins
            return servletResponse.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN).trim().equals(requestOrigin);
        }

        if (mount.getVirtualHost().getAllowedOrigins().contains(requestOrigin)) {
            log.info("Origin '{}' is whilelisted", requestOrigin);
            return true;
        }
        return false;
    }

    private boolean isAllowedOriginResponseHeaderWildcard(final HttpServletResponse servletResponse) {
        final String allowOrigin = servletResponse.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN);
        if (allowOrigin == null) {
            return false;
        }
        return allowOrigin.trim().equals("*");
    }

    private void setAccessControlAllowOrigin(final HttpServletResponse servletResponse, final String requestOrigin) {
        log.info("Request Origin '{}' is allowed", requestOrigin);

        if (isAllowedOriginResponseHeaderWildcard(servletResponse) && !replaceWildcardAllowOrigin) {
            // keep Access-Control-Allow-Origin: *
            return;
        }

        // we set the ACCESS_CONTROL_ALLOW_ORIGIN explicitly to the requested origin, even if it is already
        // set to '*' : We do this since '*' is not allowed in combination with ACCESS_CONTROL_ALLOW_CREDENTIALS: true
        servletResponse.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);

    }

    /**
     * <p> Merges the pre set headers with {@code extra} headers. For example the pre set Headers can be two Strings
     * looking for example like "foo, bar" and "bar,lux" and the extra might be ["lux","xyz"]. In that case the returned
     * result will be 'foo, bar, lux, xyz' </p>
     *
     * @param preSetHeaders current header values
     * @param extra         additional header values
     * @return merged header values
     */
    String merge(final Collection<String> preSetHeaders, final String... extra) {
        return Stream.concat(preSetHeaders.stream().filter(StringUtils::isNotBlank),
                Arrays.stream(extra).filter(StringUtils::isNotBlank))
                .map(s -> s.split(",")).flatMap(Arrays::stream).distinct()
                .map(StringUtils::trim)
                .filter(StringUtils::isNotBlank)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    @Override
    public void initialize() {
    }

    @Override
    public void destroy() {
    }

}
