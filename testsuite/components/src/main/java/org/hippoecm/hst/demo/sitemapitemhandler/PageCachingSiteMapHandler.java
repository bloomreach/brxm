/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.sitemapitemhandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.collections.map.LRUMap;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.sitemapitemhandler.AbstractHstSiteMapHandler;
import org.hippoecm.hst.util.HstRequestUtils;

/**
 * Example of a (DUMMY) simple cache handler
 * 
 * <b>Note</b> this is a dummy demo implementation and not meant for production purposes: when a page is cached, we just return response 'cached' 
 * 
 */
public class PageCachingSiteMapHandler extends AbstractHstSiteMapHandler {

    private final Map<String, DummyResponse> cache = Collections.synchronizedMap(new LRUMap(1000));
    
    @Override
    public ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request,
            HttpServletResponse response) throws HstSiteMapItemHandlerException {
        
        String key = computeKey(resolvedSiteMapItem, request);
        
        DummyResponse cached = cache.get(key);
        
        if(cached != null) {
            try {
                response.getWriter().write(cached.response);
                // done with dummy response
                return null;
            } catch (IOException e) {
               throw new HstSiteMapItemHandlerException(e);
            }
        } else {
            /* <b>Note</b> again, this is not a serious implementation!  
             * 
             * we now just do a dummy cached value
             */
            DummyResponse dummy = new DummyResponse("We have cached: " + key);
            cache.put(key,dummy);
        }
        
        // we know the request started with 'cached' if we get here
        String pathInfo = resolvedSiteMapItem.getPathInfo();
        
        if(pathInfo.startsWith("cached/")) {
            pathInfo = pathInfo.substring("cached/".length());
            ResolvedSiteMapItem redirectedItem = matchSiteMapItem(request, new HstResponseWrapper(response), resolvedSiteMapItem, pathInfo);
            return redirectedItem;
        } else {
            throw new HstSiteMapItemHandlerException("the pathInfo should start with /cached/ for this handler");
        }
        
        
    }

    private String computeKey(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        
        builder.append(HstRequestUtils.getFarthestRequestHost(request));
        builder.append(HstRequestUtils.getRequestURI(request, true));
        if(request.getQueryString() != null) {
            builder.append('?');
            builder.append(request.getQueryString());
        }
        return builder.toString();
    }

    class DummyResponse {
        String response;
        
        DummyResponse(String response) {
            this.response = response;
        }
    }
    
    
    class HstResponseWrapper extends HttpServletResponseWrapper {

        public HstResponseWrapper(HttpServletResponse response) {
            super(response);
        }

       
    }
    
}
