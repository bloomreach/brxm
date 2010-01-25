/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * HST Response Utils 
 * 
 * @version $Id$
 */
public class HstResponseUtils {
    
    private HstResponseUtils() {
        
    }
    
    /**
     * Facility method for sending a redirect to a SiteMapItemId.  
     * 
     * @param request the HstRequest
     * @param response the HstResponse
     * @param path the sitemap path you want to redirect to 
     */
    public static void sendRedirect(HstRequest request, HstResponse response, String path) {
        sendRedirect(request, response, path, null);
    }
    
    /**
     * Facility method for sending a redirect to a SiteMapItemId.  
     * 
     * @param request the HstRequest
     * @param response the HstResponse
     * @param path the sitemap path you want to redirect to 
     * @param queryParams query parameters to append to the redirection url
     */
    public static void sendRedirect(HstRequest request, HstResponse response, String path, Map<String, String []> queryParams) {
        HstRequestContext requestContext = request.getRequestContext();
        HstLinkCreator linkCreator = requestContext.getHstLinkCreator();
        HstSiteMap siteMap = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap();
        
        HstLink link = linkCreator.create(path, siteMap.getSite());
        if(link == null) {
            throw new HstComponentException("Can not redirect.");
        }
        String urlString = null;
        urlString = link.toUrlForm(request, response, false);
        
        if(urlString == null) {
            throw new HstComponentException("Can not redirect.");
        }
        
        if (queryParams != null && !queryParams.isEmpty()) {
            try {
                String characterEncoding = response.getCharacterEncoding();
                
                if (characterEncoding == null) {
                    characterEncoding = "ISO-8859-1";
                }
                
                StringBuilder urlBuilder = new StringBuilder(80).append(urlString);
                boolean firstParamDone = (urlBuilder.indexOf("?") >= 0);
                
                for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                    String name = entry.getKey();
                    
                    for (String value : entry.getValue()) {
                        urlBuilder.append(firstParamDone ? "&" : "?")
                        .append(name)
                        .append("=")
                        .append(URLEncoder.encode(value, characterEncoding));
                        
                        firstParamDone = true;
                    }
                }
                
                urlString = urlBuilder.toString();
            } catch (UnsupportedEncodingException e) {
                throw new HstComponentException(e);
            }
        }
        
        try {
            response.sendRedirect(urlString);
        } catch (IOException e) {
            throw new HstComponentException("Could not redirect. ",e);
        }
    }
    
}
