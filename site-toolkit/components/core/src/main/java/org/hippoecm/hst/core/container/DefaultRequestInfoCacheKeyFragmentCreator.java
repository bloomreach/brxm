/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.WebFileUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.WebFileBundle;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.RENDER_BRANCH_ID;

/**
 * Default {@link RequestInfoCacheKeyFragmentCreator} which takes all parts of the request to create a cachekey with.
 */
public class DefaultRequestInfoCacheKeyFragmentCreator implements RequestInfoCacheKeyFragmentCreator {

    private static final Logger log = LoggerFactory.getLogger(DefaultRequestInfoCacheKeyFragmentCreator.class);

    private Optional<String> webFilesAntiCacheValue = null;

    public Serializable create(final HstRequestContext requestContext) {
        HttpServletRequest request = requestContext.getServletRequest();
        StringBuilder requestInfo = new StringBuilder(256);
        final char delim = '\uFFFF';

        Optional<String> antiCacheValue = webFilesAntiCacheValue;
        if (antiCacheValue == null) {
            antiCacheValue = populateAntiCacheValue(requestContext);
        }

        if (antiCacheValue.isPresent()) {
            requestInfo.append(antiCacheValue.get()).append(delim);
        }

        // Implementers should differentiate between GET and HEAD requests otherwise blank pages
        //  can result.
        requestInfo.append(request.getMethod()).append(delim);
        requestInfo.append(HstRequestUtils.getFarthestRequestScheme(request)).append(delim);
        requestInfo.append(HstRequestUtils.getFarthestRequestHost(request)).append(delim);
        requestInfo.append(request.getRequestURI()).append(delim);
        requestInfo.append(StringUtils.defaultString(request.getQueryString())).append(delim);
        if (requestContext.getAttribute(RENDER_BRANCH_ID) != null) {
            requestInfo.append((String)requestContext.getAttribute(RENDER_BRANCH_ID)).append(delim);
        }

        // AFter an internal HST FORWARD, all the above parts are the same because same http request,
        // but the base URL pathInfo has been changed. Hence, we need to account for pathInfo
        // to make sure that in a FORWARDED request we do not get the same cached entry
        requestInfo.append(requestContext.getBaseURL().getPathInfo()).append(delim);

        return requestInfo.toString();
    }

    @Override
    public void reset() {
        webFilesAntiCacheValue = null;
    }


    private Optional<String> populateAntiCacheValue(final HstRequestContext requestContext) {
        Optional<String> antiCacheValue = webFilesAntiCacheValue;
        if (antiCacheValue != null) {
            return antiCacheValue;
        }
        WebFilesService service = HippoServiceRegistry.getService(WebFilesService.class);
        if (service == null) {
            antiCacheValue = Optional.empty();
            webFilesAntiCacheValue = antiCacheValue;
            return antiCacheValue;
        }
        final Session session;
        try {
            session = requestContext.getSession();
            final String bundleName = WebFileUtils.getBundleName(requestContext);
            log.debug("Trying to get web file bundle '{}' with session user '{}'", bundleName, session.getUserID());
            final WebFileBundle webFileBundle = service.getJcrWebFileBundle(session, bundleName);
            String stringAntiCacheValue = webFileBundle.getAntiCacheValue();
            if (stringAntiCacheValue == null) {
                antiCacheValue = Optional.empty();
                webFilesAntiCacheValue = antiCacheValue;
                return antiCacheValue;
            }
            antiCacheValue = Optional.of(stringAntiCacheValue);
            webFilesAntiCacheValue = antiCacheValue;
            return antiCacheValue;
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Cannot get anti-cache value. Use cache key without it.", e);
            } else {
                log.warn("Cannot get anti-cache value. Use cache key without it : {}", e.toString());
            }
            antiCacheValue = Optional.empty();
            webFilesAntiCacheValue = antiCacheValue;
            return antiCacheValue;
        }

    }

}
