/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter delegating the calls to a component defined in HST container.
 * <PRE>
 * Note: the delegatee filter component bean is responsible for initializing/destroying by itself.
 *       This delegating filter doesn't invoke <code>#init(FilterConfig)</code> or <code>#destroy()</code> methods
 *       of the delegatee filter component bean.
 *       So, in spring assembly, you should maintain its lifecycle properly if needed.
 * </PRE>
 */
public class DelegatingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DelegatingFilter.class);

    private static final String DELEGATEE_COMPONENT_NAME_INIT_PARAM = "delegatee";

    private String delegateeName;

    /**
     * @deprecated not used any more since CMS 10.0, HST 2.30.00. Instead use sitemap items with
     * hst:namedpipeline = PlainFilterChainInvokingPipeline to fall through to next servlet (filter)
     */
    @Deprecated
    public static final String PREFIX_EXCLUSIONS_INIT_PARAM = "prefixExclusions";
    /**
     * @deprecated not used any more since CMS 10.0, HST 2.30.00. Instead use sitemap items with
     * hst:namedpipeline = PlainFilterChainInvokingPipeline to fall through to next servlet (filter)
     */
    @Deprecated
    public static final String SUFFIX_EXCLUSIONS_INIT_PARAM = "suffixExclusions";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        logUnusedExclusionsInitParams(filterConfig, PREFIX_EXCLUSIONS_INIT_PARAM);
        logUnusedExclusionsInitParams(filterConfig, SUFFIX_EXCLUSIONS_INIT_PARAM);

        String param = filterConfig.getInitParameter(DELEGATEE_COMPONENT_NAME_INIT_PARAM);

        if (param != null) {
            delegateeName = param.trim();
        }
    }


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {
        Filter delegatee = getDelegatee();

        if (delegatee == null) {
            throw new ServletException("No delegatee filter found by name: " + delegateeName);
        }

        delegatee.doFilter(request, response, filterChain);
    }

    @Override
    public void destroy() {
    }

    /**
     * Returns the name of the delegatee <code>Filter</code> component bean.
     * @return
     */
    protected String getDelegateeName() {
        return delegateeName;
    }

    /**
     * Returns the delegatee <code>Filter</code> component bean.
     * @return
     */
    protected Filter getDelegatee() {
        if (!HstServices.isAvailable()) {
            log.error("The HST Container services are not initialized yet.");
            return null;
        }

        String delegateeComponentName = getDelegateeName();

        if (delegateeComponentName == null) {
            log.error("The delegatee filter component name is not set.");
            return null;
        } else {
            return HstServices.getComponentManager().getComponent(delegateeComponentName);
        }
    }

    private void logUnusedExclusionsInitParams(final FilterConfig filterConfig, String initParam) {
        if (filterConfig.getInitParameter(initParam) != null ||
                filterConfig.getServletContext().getInitParameter(initParam) != null) {
            log.warn("{} init param is not used any more. Use a (hst:default) sitemap item to account for prefixes/suffixes " +
                    "that need special handling or use hst-config.properties to set comma separated list for filter.prefix.exclusions or " +
                    "filter.suffix.exclusions.", initParam);
        }
    }
}
