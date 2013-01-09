/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public class ConfigurationViewUtilities {

    public static final String SMALL_INDENT = "  ";
    public static final String INDENT = "\t";
    
    public static final void view(StringBuffer buf, HstSite site) {
        view(buf, "", site);
    }
    
    public static final void view(StringBuffer buf, String indent, HstSite site) {
        if(site == null) {
            buf.append("\n").append(indent).append("+HstSite: null");
            return;
        }
        buf.append("\n").append(indent).append("+Site: ").append(site.getName()).append(" (").append(site.hashCode()).append(")");
        indent = indent + SMALL_INDENT;
        buf.append("\n").append(indent).append("-Identifier : ").append(site.getCanonicalIdentifier());
       
        view(buf, indent+SMALL_INDENT, site.getSiteMap()) ;
        
        view(buf, indent+SMALL_INDENT, site.getComponentsConfiguration()) ;
    }
    

    public static final void view(StringBuffer buf,  HstSiteMap hstSiteMap) {
        view(buf, "", hstSiteMap);
    }
    
    public static final void view(StringBuffer buf, String indent,  HstSiteMap hstSiteMap) {
        if(hstSiteMap == null) {
            buf.append("\n").append(indent).append("+HstSiteMap: null");
            return;
        }
        buf.append("\n").append(indent).append("+HstSiteMap: (").append(hstSiteMap.hashCode()).append(")");
        for(HstSiteMapItem siteMapItem : hstSiteMap.getSiteMapItems()) {
            view(buf, indent+SMALL_INDENT, siteMapItem);
        }
    }
    
    public static final void view(StringBuffer buf,  HstSiteMapItem hstSiteMapItem) {
        view(buf, "", hstSiteMapItem);
    }
    
    public static final void view(StringBuffer buf, String indent,  HstSiteMapItem hstSiteMapItem) {
        if(hstSiteMapItem == null) {
            buf.append("\n").append(indent).append("+HstSiteMapItem: null");
            return;
        }
        buf.append("\n").append(indent).append("+HstSiteMapItem: (").append(hstSiteMapItem.hashCode()).append(")");
        String newLine = "\n" + indent + SMALL_INDENT + "-";
        buf.append(newLine).append("id = ").append(hstSiteMapItem.getId());
        buf.append(newLine).append("value = ").append(hstSiteMapItem.getValue());
        buf.append(newLine).append("relativecontentpath = ").append(hstSiteMapItem.getRelativeContentPath());
        buf.append(newLine).append("componentconfigurationid = ").append(hstSiteMapItem.getComponentConfigurationId());
        buf.append(newLine).append("portletcomponentconfigurationid = ").append(hstSiteMapItem.getPortletComponentConfigurationId());
        buf.append(newLine).append("iswildcard = ").append(hstSiteMapItem.isWildCard());
        for(HstSiteMapItem siteMapItem : hstSiteMapItem.getChildren()) {
            view(buf, indent+SMALL_INDENT, siteMapItem);
        }
    }
    
    public static final void view(StringBuffer buf,  HstComponentsConfiguration hstComponentsConfiguration) {
        view(buf, "", hstComponentsConfiguration);
    }
    
    public static final void view(StringBuffer buf, String indent,  HstComponentsConfiguration hstComponentsConfiguration) {
        if(hstComponentsConfiguration == null) {
            buf.append("\n").append(indent).append("+HstComponentsConfiguration: null");
            return;
        }
        buf.append("\n").append(indent).append("+HstComponentsConfiguration: (").append(hstComponentsConfiguration.hashCode()).append(")");
        
        for(HstComponentConfiguration hstComponentConfiguration : hstComponentsConfiguration.getComponentConfigurations().values()) {
            view(buf, indent+SMALL_INDENT, hstComponentConfiguration);
        }
        
    }
    
    public static final void view(StringBuffer buf,  HstComponentConfiguration hstComponentConfiguration) {
        view(buf, "", hstComponentConfiguration);
    }
    
    public static final void view(StringBuffer buf, String indent,  HstComponentConfiguration hstComponentConfiguration) {
        if(hstComponentConfiguration == null) {
            buf.append("\n").append(indent).append("+HstComponentConfiguration: null");
            return;
        }
        buf.append("\n").append(indent).append("+HstComponentConfiguration: (").append(hstComponentConfiguration.hashCode()).append(")");
        String newLine = "\n" + indent + SMALL_INDENT + "-";
        buf.append(newLine).append("id = ").append(hstComponentConfiguration.getId());
        buf.append(newLine).append("referencename = ").append(hstComponentConfiguration.getReferenceName());
        buf.append(newLine).append("componentclassname = ").append(hstComponentConfiguration.getComponentClassName());
        buf.append(newLine).append("renderpath = ").append(hstComponentConfiguration.getRenderPath());
        buf.append(newLine).append("parameters = ").append(hstComponentConfiguration.getParameters());
        for(HstComponentConfiguration childConfiguration : hstComponentConfiguration.getChildren().values()) {
            view(buf, indent+SMALL_INDENT, childConfiguration);
        }
    }
    

}
