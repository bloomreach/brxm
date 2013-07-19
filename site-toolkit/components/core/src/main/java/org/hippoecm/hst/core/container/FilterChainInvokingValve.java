/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.container.HstContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * FilterChainInvokingValve
 * <P>
 * Unwraps the request to the default request provided by the servlet container and invokes
 * <code>FilterChain#doFilter(ServletRequest, ServletResponse)</code> in order to let external servlet application
 * continue request processing with HST provided context attributes.
 * </P>
 */
public class FilterChainInvokingValve extends AbstractBaseOrderableValve {

    private static Logger log = LoggerFactory.getLogger(FilterChainInvokingValve.class);

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest request = context.getServletRequest();
        HttpServletResponse response = context.getServletResponse();
        FilterChain filterChain = (FilterChain) request.getAttribute(ContainerConstants.HST_FILTER_CHAIN);

        try {
            // NOTE: It doesn't work properly if the wrapped request (HstContainerRequestImpl) is passed.
            //       There might be some missing attributes or properties in the wrapped request from the perspective of
            //       external application framework such as Spring MVC.
            //       Therefore, for now at least, let's unwrap the request to continue with the filter chain.
            //       Even though unwrapped, all the request attributes are preserved in the original request instance.
            //       (Refer to the HstContainerRequest implementation.)
            ServletRequest unwrappedRequest = request;

            while (unwrappedRequest instanceof HttpServletRequestWrapper) {
                unwrappedRequest = ((HttpServletRequestWrapper) request).getRequest();

                // Let's stop when the HST specific request wrapper has been unwrapped.
                if (request instanceof HstContainerRequest) {
                    break;
                }
            }

            filterChain.doFilter(unwrappedRequest, response);
        } catch (Exception e) {
            log.warn("Failed to continue with the filterChain.", e);
        }

        context.invokeNext();
    }

}
