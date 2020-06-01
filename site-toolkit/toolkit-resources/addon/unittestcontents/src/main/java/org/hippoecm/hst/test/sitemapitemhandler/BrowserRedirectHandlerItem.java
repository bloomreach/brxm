/*
 * Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;

public class BrowserRedirectHandlerItem extends AbstractTestHstSiteItemMapHandler {


  
    public ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request,
            HttpServletResponse response) throws HstSiteMapItemHandlerException {
        
       String redirect =  handlerConfig.getProperty("unittestproject:redirectto", resolvedSiteMapItem, String.class);
       try {
          response.sendRedirect(redirect);
       } catch (IOException e) {
          throw new HstSiteMapItemHandlerException(e);
       }
      return null;
    }

}
