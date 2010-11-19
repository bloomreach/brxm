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
package org.hippoecm.hst.configuration.sitemenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMenuItemConfigurationService implements HstSiteMenuItemConfiguration {

    private static final Logger log = LoggerFactory.getLogger(HstSiteMenuItemConfigurationService.class);
    
    private static final String PARENT_PROPERTY_PLACEHOLDER = "${parent}";
    
    private HstSiteMenuConfiguration hstSiteMenuConfiguration;
    private HstSiteMenuItemConfiguration parent;
    private String name;
    private List<HstSiteMenuItemConfiguration> childItems = new ArrayList<HstSiteMenuItemConfiguration>();
    private String siteMapItemPath;
    private String externalLink;
    private int depth;
    private boolean repositoryBased;
    private Map<String, Object> properties;
    private Map<String,String> parameters = new HashMap<String,String>();
    private Map<String,String> localParameters = new HashMap<String,String>();
    
    public HstSiteMenuItemConfigurationService(HstNode siteMenuItem, HstSiteMenuItemConfiguration parent, HstSiteMenuConfiguration hstSiteMenuConfiguration) throws ServiceException {
        this.parent = parent;
        this.hstSiteMenuConfiguration = hstSiteMenuConfiguration;
        
        this.name = siteMenuItem.getValueProvider().getName();
        if(siteMenuItem.getValueProvider().hasProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_EXTERNALLINK)) {
            this.externalLink = siteMenuItem.getValueProvider().getString(HstNodeTypes.SITEMENUITEM_PROPERTY_EXTERNALLINK);
        }else if(siteMenuItem.getValueProvider().hasProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM)) {
           // siteMapItemPath can be an exact path to a sitemap item, but can also be a path to a sitemap item containing wildcards.
           this.siteMapItemPath = siteMenuItem.getValueProvider().getString(HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM);
           if(siteMapItemPath != null && siteMapItemPath.indexOf(PARENT_PROPERTY_PLACEHOLDER) > -1 ) {
                 if(parent == null || parent.getSiteMapItemPath() == null) {
                     log.error("Cannot use '{}' for a sitemenu item that does not have a parent or a parent without sitemap item path. Used for: '{}'", PARENT_PROPERTY_PLACEHOLDER, name);
                 } else {
                     siteMapItemPath = siteMapItemPath.replace(PARENT_PROPERTY_PLACEHOLDER, parent.getSiteMapItemPath());
                 }
           }
        } else {
           log.info("HstSiteMenuItemConfiguration cannot be used for linking because no associated HstSiteMapItem present"); 
        }
        
        if(siteMenuItem.getValueProvider().hasProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_REPOBASED)) {
            this.repositoryBased = siteMenuItem.getValueProvider().getBoolean(HstNodeTypes.SITEMENUITEM_PROPERTY_REPOBASED);
        }
        
        if(siteMenuItem.getValueProvider().hasProperty(HstNodeTypes.SITEMENUITEM_PROPERTY_DEPTH)) {
           this.depth = siteMenuItem.getValueProvider().getLong(HstNodeTypes.SITEMENUITEM_PROPERTY_DEPTH).intValue();
        }
        
        if( (this.repositoryBased && this.depth <= 0) || (!this.repositoryBased && this.depth > 0) ) {
            this.repositoryBased =false;
            this.depth = 0;
            log.warn("Ambiguous configuration for repository based sitemenu: only when both repository based is true AND " +
                    "depth > 0 the configuration is correct for repository based navigation. Skipping repobased and depth setting for this item.");
        }
        
        this.properties = siteMenuItem.getValueProvider().getProperties();
        

        String[] parameterNames = siteMenuItem.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES);
        String[] parameterValues = siteMenuItem.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES);
        
        if(parameterNames != null && parameterValues != null){
           if(parameterNames.length != parameterValues.length) {
               log.warn("Skipping parameters for component because they only make sense if there are equal number of names and values");
           }  else {
               for(int i = 0; i < parameterNames.length ; i++) {
                   this.parameters.put(parameterNames[i], parameterValues[i]);
                   this.localParameters.put(parameterNames[i], parameterValues[i]);
               }
           }
        }
        
        if(this.parent != null){
            // add the parent parameters that are not already present
            for(Entry<String, String> parentParam : this.parent.getParameters().entrySet()) {
                if(!this.parameters.containsKey(parentParam.getKey())) {
                    this.parameters.put(parentParam.getKey(), parentParam.getValue());
                }
            }
        }
        
        for(HstNode childItem : siteMenuItem.getNodes()) {
            HstSiteMenuItemConfiguration child = new HstSiteMenuItemConfigurationService(childItem, this, this.hstSiteMenuConfiguration);
            childItems.add(child);
        }
        
    }

    public List<HstSiteMenuItemConfiguration> getChildItemConfigurations() {
        return Collections.unmodifiableList(childItems);
    }

    public String getName() {
        return this.name;
    }

    public HstSiteMenuItemConfiguration getParentItemConfiguration() {
        return this.parent;
    }

    public HstSiteMenuConfiguration getHstSiteMenuConfiguration() {
        return this.hstSiteMenuConfiguration;
    }

    public String getSiteMapItemPath() {
        return this.siteMapItemPath;
    }

    public String getExternalLink() {
        return this.externalLink;
    }
    public int getDepth() {
        return this.depth;
    }

    public boolean isRepositoryBased() {
        return this.repositoryBased;
    }

    public String getParameter(String name) {
        return this.parameters.get(name);
    }
    

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(this.parameters);
    }
    
    public String getLocalParameter(String name) {
        return this.localParameters.get(name);
    }

    public Map<String, String> getLocalParameters() {
        return Collections.unmodifiableMap(this.localParameters);
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }


}
