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
package org.hippoecm.hst.site.request;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

public class ResolvedSiteMapItemImpl implements ResolvedSiteMapItem{
    
    private HstSiteMap hstSiteMap;
    private Map<String, Object> resolvedProperties;
    private String[] params;
    
    public ResolvedSiteMapItemImpl(HstSiteMapItem hstSiteMapItem , String[] params) {
       this.hstSiteMap = hstSiteMapItem.getHstSiteMap();
       this.resolvedProperties = new HashMap<String, Object>();
       this.params = params;
       
       /*
        * We take the properties form the hstSiteMapItem getProperties and replace params (like $1) with the params[] array 
        */
       
       this.resolvedProperties = hstSiteMapItem.getProperties();
       
       
       
    }
    
    public Object getProperty(String name) {
        return resolvedProperties.get(name);
    }

    public HstSiteMap getHstSiteMap() {
        return this.hstSiteMap;
    }

    
  
}
