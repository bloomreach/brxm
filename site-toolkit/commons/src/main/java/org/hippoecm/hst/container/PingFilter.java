/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.container.PingFilter.AvailabilityCheck.hstConfigNodes;

public class PingFilter implements Filter {

    // on purpose camel casing because reflect init-param values
    public enum AvailabilityCheck {
        hstConfigNodes,
        hstServices,
        repositoryAvailability
    }

    private static final Logger log = LoggerFactory.getLogger(PingFilter.class);

    /**
     * attribute on the request which gets the message stored, useful if for example in your web.xml you want to
     * include a custom 503.jsp outputting that the HST application is starting up
     */
    public final String PING_FILTER_MESSAGE_ATTR = PingFilter.class.getName() + ".msg";

    /**
     * <p>
     *     FilterConfig or ServletContext init parameter in the web.xml that indicates which check this {@code PingFilter}
     *     should do (aka meaning the HST available for serving webpages).
     * </p>
     * <p>
     *     By default, the check is based on whether the HST Configuration JCR Nodes all have been loaded into memory.
     *     (indicated by {@link HstServices#isHstConfigurationNodesLoaded()} returning {@code true}.
     *     This is the best check because it means the HST only needs to build its in memory model and does not need to
     *     fetch all jcr Nodes any more from the repository.
     * </p>
     * <p>
     *     Supported check values are:
     *     <ul>
     *         <li>hstConfigNodes</li>
     *         <li>hstServices</li>
     *         <li>repositoryAvailability</li>
     *     </ul>
     * </p>
     * <p>
     *     For example, the PingFilter check can be set to
     *     <pre>
     *         <code>
     *               <context-param>
     *                  <param-name>hst-availability-check</param-name>
     *                  <param-value>hstServices</param-value>
     *                </context-param>
     *         </code>
     *     </pre>
     * </p>
     * <p>
     *     The check for 'hstServices' complies if {@link HstServices#isAvailable()} returns {@code true}. The check for
     *     'repositoryAvailability' complies if HippoServiceRegistry.getService(RepositoryService.class) does not return
     *     null.
     * </p>
     * <p>
     *     In case the check does not comply (for example, the HST configuration JCR nodes have not yet been loaded), this
     *     PingFilter returns a response with status {@link HttpServletResponse#SC_SERVICE_UNAVAILABLE}. After the check
     *     complies, this PingFilter will return a 200.
     * </p>
     * <p>
     *     Note this PingFilter should be placed <strong>before</strong> the {@link HstFilter} in the web.xml
     * </p>
     *
     */
    public final String AVAILABILITY_CHECK_PARAM = "hst-availability-check";

    private AvailabilityCheck availabilityCheck;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        final String availabilityCheckValue = getInitParameter(filterConfig, filterConfig.getServletContext(), AVAILABILITY_CHECK_PARAM, hstConfigNodes.name());
        try {
            availabilityCheck = Enum.valueOf(AvailabilityCheck.class, availabilityCheckValue);
        } catch (Exception e) {
            log.error(String.format("Illegal value '%s' for init-param '%s'. Using default value '%s'", availabilityCheckValue, AVAILABILITY_CHECK_PARAM, hstConfigNodes.name()));
            availabilityCheck = hstConfigNodes;
        }
        log.info("Availability check will be done on {}", availabilityCheck);
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse res = (HttpServletResponse)response;
        switch (availabilityCheck) {
            case hstConfigNodes:
                if (HstServices.isHstConfigurationNodesLoaded()) {
                    available(request, res);
                    return;
                }
                break;
            case hstServices:
                if (HstServices.isAvailable()) {
                    available(request, res);
                    return;
                }
                break;
            case repositoryAvailability:
                if (HippoServiceRegistry.getService(RepositoryService.class) != null) {
                    available(request, res);
                    return;
                }
                break;
        }
        request.setAttribute(PING_FILTER_MESSAGE_ATTR, "HST Application is starting up");
        res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    private void available(final ServletRequest request, final HttpServletResponse response) throws IOException {
        final String msg = "HST Application is ready to serve requests";
        request.setAttribute(PING_FILTER_MESSAGE_ATTR, msg);
        response.setContentType("text/plain");
        response.getWriter().println(msg);
        response.setStatus(HttpServletResponse.SC_OK);
        return;
    }

    @Override
    public void destroy() {

    }

    public static String getInitParameter(FilterConfig filterConfig, ServletContext servletContext, String paramName, String defaultValue) {
        String value = null;

        if (filterConfig != null) {
            value = filterConfig.getInitParameter(paramName);
        }

        if (value == null && servletContext != null) {
            value = servletContext.getInitParameter(paramName);
        }

        if (value == null) {
            value = defaultValue;
        }

        return value;
    }
}
