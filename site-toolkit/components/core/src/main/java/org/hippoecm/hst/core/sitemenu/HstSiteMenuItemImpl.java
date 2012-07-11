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
package org.hippoecm.hst.core.sitemenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMenuItemImpl extends AbstractMenuItem implements HstSiteMenuItem {
    
    private static Logger log = LoggerFactory.getLogger(HstSiteMenuItemImpl.class);

    private HstSiteMenu hstSiteMenu;
    private List<HstSiteMenuItem> hstSiteMenuItems = new ArrayList<HstSiteMenuItem>();
    private HstSiteMenuItem parent;
    private HstLinkCreator linkCreator;
    private ResolvedSiteMapItem resolvedSiteMapItem;
    private HstSiteMenuItemConfiguration hstSiteMenuItemConfiguration;
    private String hstSiteMapItemRefId;
    private String hstSiteMapItemPath;
    private String externalLink;
    private Mount targetMount;
    
    
    public HstSiteMenuItemImpl(HstSiteMenu hstSiteMenu, HstSiteMenuItem parent, HstSiteMenuItemConfiguration hstSiteMenuItemConfiguration, HstRequestContext hstRequestContext) {
        this.hstSiteMenu = hstSiteMenu;
        this.parent = parent;
        this.hstSiteMenuItemConfiguration = hstSiteMenuItemConfiguration;
        this.externalLink = hstSiteMenuItemConfiguration.getExternalLink();
        this.linkCreator = hstRequestContext.getHstLinkCreator();
        this.name = hstSiteMenuItemConfiguration.getName();
        this.depth = hstSiteMenuItemConfiguration.getDepth();
        this.repositoryBased = hstSiteMenuItemConfiguration.isRepositoryBased();
        this.properties = hstSiteMenuItemConfiguration.getProperties();
        // if there is an hst:mountalias, we use the mount belonging to that alias. If there is no mount alias defined,
        // we use the mount from the request context
        
        if (hstSiteMenuItemConfiguration.getMountAlias() != null) {
            targetMount = hstRequestContext.getMount(hstSiteMenuItemConfiguration.getMountAlias());
            if(targetMount == null) {
                log.warn("Cannot create links for sitemenu item '"+name+"' of menu '"+hstSiteMenu.getName()+"' because could not lookup mount with alias '{}' for current mount '{}'", hstSiteMenuItemConfiguration.getMountAlias(), hstRequestContext.getResolvedMount().getMount());
            }
        } else {
            targetMount = hstRequestContext.getResolvedMount().getMount();
        }
        String siteMapItemRefIdOrPath = PathUtils.normalizePath(hstSiteMenuItemConfiguration.getSiteMapItemPath());
        
        if (targetMount != null) {
            HstSiteMapItem siteMapItemByRefId = targetMount.getHstSite().getSiteMap().getSiteMapItemByRefId(siteMapItemRefIdOrPath);
            
            if (siteMapItemByRefId != null) {
                hstSiteMapItemRefId = siteMapItemRefIdOrPath;
                hstSiteMapItemPath = HstSiteMapUtils.getPath(siteMapItemByRefId);

                log.debug("sitemapitem of sitemenu, '{}', found by refid, '{}'. sitemapitem path: " + hstSiteMapItemPath, name, siteMapItemRefIdOrPath);
            } else {
                hstSiteMapItemPath = siteMapItemRefIdOrPath;

                log.debug("sitemapitem of sitemenu, '{}', will be found by path, '{}'.", name, siteMapItemRefIdOrPath);
            }
        }
        
        for(HstSiteMenuItemConfiguration childItemConfiguration : hstSiteMenuItemConfiguration.getChildItemConfigurations()) {
            hstSiteMenuItems.add(new HstSiteMenuItemImpl(hstSiteMenu, this, childItemConfiguration, hstRequestContext));
        }
        resolvedSiteMapItem = hstRequestContext.getResolvedSiteMapItem();
        
        String currentPathInfo = resolvedSiteMapItem.getPathInfo();
        
        if (hstSiteMapItemPath != null && currentPathInfo != null) {
            
            if (hstSiteMapItemPath.equals(currentPathInfo)) {
                // the current HstSiteMenuItem is selected. Set it to selected, and also set all the ancestors selected
                this.selected = true;
                ((HstSiteMenuImpl)hstSiteMenu).setSelectedSiteMenuItem(this);
            }
            
            if(currentPathInfo.startsWith(hstSiteMapItemPath)) {
                // check if the match was until a slash, otherwise it is not a sitemenu item we want to expand
                String sub = currentPathInfo.substring(hstSiteMapItemPath.length());
                
                if("".equals(sub) || sub.startsWith("/")) {
                    // not selected but expand all ancestors
                    this.expanded = true;
                    if (this.parent == null) {
                        // we are a root HstSiteMenuItem. Set selected to the HstSiteMenu container of this item
                        ((HstSiteMenuImpl) this.hstSiteMenu).setExpanded();
                    } else {
                        ((HstSiteMenuItemImpl) this.parent).setExpanded();
                    }
                }
            }
        }
    }

    public List<HstSiteMenuItem> getChildMenuItems() {
        return hstSiteMenuItems;
    }

    public HstLink getHstLink() {
        if(targetMount == null) {
            log.warn("Cannot create link for sitemenu item '{}' of menu '{}' because target mount is null. Return null", name, hstSiteMenu.getName());
            return null;
        }
        if (hstSiteMapItemRefId != null) {
            return linkCreator.createByRefId(hstSiteMapItemRefId, targetMount);
        } else if (hstSiteMapItemPath != null) {
            return linkCreator.create(hstSiteMapItemPath, targetMount);
        }
        if (externalLink == null) {
            log.warn("Sitemenu item '{}' of menu '{}' does not contain an hstSiteMapItemRefId, an hstSiteMapItemPath or an externalLink. Cannot create link for sitemenu item, return null", name, hstSiteMenu.getName());
        }
        return null;
    }
    
    public String getExternalLink() {
        return this.externalLink;
    }

    public HstSiteMenuItem getParentItem() {
        return this.parent;
    }
    
    public void setExpanded(){
       this.expanded = true;
       if (this.parent == null) {
           // we are a root HstSiteMenuItem. Set selected to the HstSiteMenu container of this item
           ((HstSiteMenuImpl) this.hstSiteMenu).setExpanded();
       } else {
           ((HstSiteMenuItemImpl) this.parent).setExpanded();
       }
    }
    
    public HstSiteMenu getHstSiteMenu() {
        return this.hstSiteMenu;
    }

    public int getDepth() {
        return this.depth;
    }

    public boolean isRepositoryBased() {
        return this.repositoryBased;
    }
    
    public HstSiteMenuItem getDeepestExpandedItem(){
        for(HstSiteMenuItem item : hstSiteMenuItems) {
            if(item.isExpanded()) {
                return ((HstSiteMenuItemImpl)item).getDeepestExpandedItem();
            }
        }
        return this;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }


    public Map<String,String> getParameters() {
        Map<String,String> parameters = new HashMap<String, String>();
        PropertyParser pp = new PropertyParser(resolvedSiteMapItem.getParameters());
        for(Entry<String, String> entry: hstSiteMenuItemConfiguration.getParameters().entrySet()) {
            String parsedParamValue = (String)pp.resolveProperty(entry.getKey(), entry.getValue());
            parameters.put(entry.getKey(), parsedParamValue);
        }
        return parameters;
    }
    
   
    public String getParameter(String name) {
        String paramValue = hstSiteMenuItemConfiguration.getParameter(name);
        PropertyParser pp = new PropertyParser(resolvedSiteMapItem.getParameters());
        String parsedParamValue = (String)pp.resolveProperty(name, paramValue);
        return parsedParamValue;
    }
    
    public Map<String,String> getLocalParameters() {
        Map<String,String> parameters = new HashMap<String, String>();
        PropertyParser pp = new PropertyParser(resolvedSiteMapItem.getParameters());
        for(Entry<String, String> entry: hstSiteMenuItemConfiguration.getLocalParameters().entrySet()) {
            String parsedParamValue = (String)pp.resolveProperty(entry.getKey(), entry.getValue());
            parameters.put(entry.getKey(), parsedParamValue);
        }
        return parameters;
    }
    
    public String getLocalParameter(String name) {
        String paramValue = hstSiteMenuItemConfiguration.getLocalParameter(name);
        PropertyParser pp = new PropertyParser(resolvedSiteMapItem.getParameters());
        String parsedParamValue = (String)pp.resolveProperty(name, paramValue);
        return parsedParamValue;
    }
}
