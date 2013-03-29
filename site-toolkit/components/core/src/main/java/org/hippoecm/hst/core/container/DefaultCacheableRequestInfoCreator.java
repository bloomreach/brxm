/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;

/**
 * DefaultCacheableRequestInfoCreator
 */
public class DefaultCacheableRequestInfoCreator implements CacheableRequestInfoCreator {

    public Serializable createRequestInfo(HstRequestContext requestContext) {
        HttpServletRequest request = requestContext.getServletRequest();
        StringBuilder requestInfo = new StringBuilder(256);
        final char delim = '\uFFFF';
        // Implementers should differentiate between GET and HEAD requests otherwise blank pages
        //  can result.
        requestInfo.append(request.getMethod()).append(delim);
        requestInfo.append(HstRequestUtils.getFarthestRequestHost(request)).append(delim);
        requestInfo.append(request.getRequestURI()).append(delim);
        requestInfo.append(request.getQueryString()).append(delim);

        // AFter an internal HST FORWARD, all the above parts are the same because same http request,
        // but the base URL pathInfo has been changed. Hence, we need to account for pathInfo
        // to make sure that in a FORWARDED request we do not get the same cached entry
        requestInfo.append(requestContext.getBaseURL().getPathInfo()).append(delim);
        return requestInfo.toString();
    }

}
