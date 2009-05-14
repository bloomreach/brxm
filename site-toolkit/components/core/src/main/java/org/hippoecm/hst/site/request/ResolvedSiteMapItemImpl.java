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

import java.util.Properties;
import java.util.Map.Entry;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolvedSiteMapItemImpl implements ResolvedSiteMapItem{

    private final static Logger log = LoggerFactory.getLogger(ResolvedSiteMapItemImpl.class);
    private HstSiteMapItem hstSiteMapItem;
    private Properties resolvedParameters;
    private String relativeContentPath;
    private HstComponentConfiguration hstComponentConfiguration;
    private String pathInfo;
    
    public ResolvedSiteMapItemImpl(HstSiteMapItem hstSiteMapItem , Properties params, String pathInfo) {
       HstSite hstSite = hstSiteMapItem.getHstSiteMap().getSite();
       this.pathInfo = PathUtils.normalizePath(pathInfo);
       this.hstSiteMapItem = hstSiteMapItem;
       if(hstSiteMapItem.getComponentConfigurationId() == null) {
           log.warn("ResolvedSiteMapItemImpl cannot be created correctly, because the sitemap item '{}' does not have a component configuration id.", hstSiteMapItem.getId());
       } else {
           this.hstComponentConfiguration = hstSite.getComponentsConfiguration().getComponentConfiguration(hstSiteMapItem.getComponentConfigurationId());
           if(hstComponentConfiguration == null) {
               log.warn("ResolvedSiteMapItemImpl cannot be created correctly, because the component configuration id cannot be found.", hstSiteMapItem.getComponentConfigurationId());
           }
       }
       
       /*
        * We take the properties form the hstSiteMapItem getParameters and replace params (like ${1}) with the params[] array 
        */
       
       this.resolvedParameters = new Properties();
       
       resolvedParameters.putAll(params);
       
       PropertyParser pp = new PropertyParser(params);
       
       for(Entry<String, String> entry : hstSiteMapItem.getParameters().entrySet()) {
           Object o = pp.resolveProperty(entry.getKey(), entry.getValue());
           resolvedParameters.put(entry.getKey(), o);
       }
       relativeContentPath = (String)pp.resolveProperty("relativeContentPath", hstSiteMapItem.getRelativeContentPath());

    }
    
    public int getStatusCode(){
        return this.hstSiteMapItem.getStatusCode();
    }
    
    public HstSiteMapItem getHstSiteMapItem() {
        return this.hstSiteMapItem;
    }
    
    public HstComponentConfiguration getHstComponentConfiguration() {
        return this.hstComponentConfiguration;
    }

    public String getParameter(String name) {
        return (String)resolvedParameters.get(name);
    }
    
    public Properties getParameters(){
        return this.resolvedParameters;
    }


    public String getRelativeContentPath() {
        return relativeContentPath;
    }

    public String getPathInfo() {
        return this.pathInfo;
    }

}
