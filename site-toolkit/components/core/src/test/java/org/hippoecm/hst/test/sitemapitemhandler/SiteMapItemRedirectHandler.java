/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.test.sitemapitemhandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;

public class SiteMapItemRedirectHandler extends AbstractTestHstSiteItemMapHandler {

  
    public ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request,
            HttpServletResponse response) throws HstSiteMapItemHandlerException {
        
       String redirect =  handlerConfig.getProperty("unittestproject:redirecttopath", resolvedSiteMapItem, String.class);
       if(redirect == null) {
           throw new HstSiteMapItemHandlerException("Cannot redirect because the property 'unittestproject:redirectto' returns null");
       }
       
       ResolvedSiteMapItem newResolvedSiteMapItem = resolveToNewSiteMapItem(request, response, resolvedSiteMapItem, redirect);
       
       if(newResolvedSiteMapItem == null) {
           throw new HstSiteMapItemHandlerException("Cannot redirect to '"+redirect+"' because this cannot be resolved to a sitemapitem");
       }
       
       return newResolvedSiteMapItem;
    }


}
