/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NoContentValve 
 * When this valve is used, it does not make sense to also have valves that write content to the {@link HttpServletResponse}
 * since this valve sets servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT)
 * @deprecated since 2.24.08 the NoopPipeline uses {@link NotFoundValve}
 */
@Deprecated 
public class NoContentValve extends AbstractBaseOrderableValve {

    private final static Logger log = LoggerFactory.getLogger(NoContentValve.class);

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HttpServletResponse servletResponse = context.getServletResponse();
        String url = HstRequestUtils.getFarthestRequestHost(context.getServletRequest()) +  context.getServletRequest().getRequestURI();
        if (!StringUtils.isEmpty(context.getServletRequest().getQueryString())) {
            url += "?" + context.getServletRequest().getQueryString();
        }
        log.warn("Return HttpServletResponse.SC_NO_CONTENT (204) because NoopPipeline was invoked for request {}", url);
        servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
        // do not call invoke next as we already return no content
    }
}
