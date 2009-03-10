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
package org.hippoecm.hst.configuration.sitemap;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.provider.PropertyMap;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMapItemService extends AbstractJCRService implements HstSiteMapItem, Service{

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(HstSiteMapItemService.class);

    private Map<String, HstSiteMapItem> childSiteMapItems = new HashMap<String, HstSiteMapItem>();

    private String siteMapRootNodePath;
    
    private String id;
    
    private String value;
    
    private String path;
    
    private String parameterizedPath;
    
    private int occurences;
    
    private String relativeContentPath;
    
    private String componentConfigurationId;
    
    private List<String> roles;

    private boolean isWildCard;
    
    private boolean isAny;
    
    private PropertyMap propertyMap;
    
    private HstSiteMap hstSiteMap;
    
    private HstSiteMapItemService parentItem;
    
    private boolean isRepositoryBased;
    
    private boolean isVisible;
    
    private List<HstSiteMapItemService> containsWildCardChildSiteMapItems = new ArrayList<HstSiteMapItemService>();
    private List<HstSiteMapItemService> containsAnyChildSiteMapItems = new ArrayList<HstSiteMapItemService>();
    private boolean containsAny;
    private boolean containsWildCard;
    private String postfix; 
    private String prefix; 
    
    public HstSiteMapItemService(Node jcrNode, String siteMapRootNodePath, HstSiteMapItem parentItem, HstSiteMap hstSiteMap) throws ServiceException{
        super(jcrNode);
        this.parentItem = (HstSiteMapItemService)parentItem;
        this.hstSiteMap = hstSiteMap; 
        String nodePath = getValueProvider().getPath();
        if(!getValueProvider().getPath().startsWith(siteMapRootNodePath)) {
            throw new ServiceException("Node path of the sitemap cannot start without the global sitemap root path. Skip SiteMapItem");
        }

        this.siteMapRootNodePath = siteMapRootNodePath;
        // path & id are the same
        this.id = this.path = nodePath.substring(siteMapRootNodePath.length()+1);
        // currently, the value is always the nodename
        this.value = getValueProvider().getName();
        
        if(this.value == null){
            log.error("The 'value' of a SiteMapItem is not allowed to be null: '{}'", nodePath);
            throw new ServiceException("The 'value' of a SiteMapItem is not allowed to be null. It is so for '"+nodePath+"'");
        }
        if(parentItem != null) {
            this.parameterizedPath = this.parentItem.getParameterizedPath()+"/";
            this.occurences = this.parentItem.getWildCardAnyOccurences();
        } else {
            parameterizedPath = "";
        }
        if(WILDCARD.equals(value)) {
            occurences++; 
            parameterizedPath = parameterizedPath + "${" + occurences + "}";
            this.isWildCard = true;
        } else if(ANY.equals(value)) {
            occurences++;
            parameterizedPath = parameterizedPath + "${" + occurences + "}";
            this.isAny = true;
        } else if(value.indexOf(WILDCARD) > -1) {
            this.containsWildCard = true;
            this.postfix = value.substring(value.indexOf(WILDCARD) + WILDCARD.length());
            this.prefix = value.substring(0, value.indexOf(WILDCARD));
            if(parentItem != null) {
                ((HstSiteMapItemService)parentItem).addWildCardPrefixedChildSiteMapItems(this);
            }
            occurences++;
            parameterizedPath = parameterizedPath + value.replace(WILDCARD, "${"+occurences+"}" );
        } else if(value.indexOf(ANY) > -1) {
            this.containsAny = true;
            this.postfix = value.substring(value.indexOf(ANY) + ANY.length());
            this.prefix = value.substring(0, value.indexOf(ANY));
            if(parentItem != null) {
                ((HstSiteMapItemService)parentItem).addAnyPrefixedChildSiteMapItems(this);
            }
            occurences++;
            parameterizedPath = parameterizedPath + value.replace(ANY, "${"+occurences+"}" );
        }
        else {
            parameterizedPath = parameterizedPath + value;
        }
        
        System.out.println(" parameterizedPath " + parameterizedPath );
        
        try {
            Node n = this.getValueProvider().getJcrNode();
            if(n.isNodeType(Configuration.SITEMAPITEM_MIXIN_PARTOFMENU)) {
                if(this.isWildCard() || containsAny || containsWildCard) {
                    log.warn("Setting isvisible mixin on a wildcard (*) sitemap item has no meaning. Skipping");
                } else if(this.getParentItem()!= null) {
                    if(this.getParentItem().isVisible()) {
                        this.isVisible = true;
                    } else {
                        log.warn("SiteMapItem '{}' cannot be visible if parent item is not visible. Ignore visible", nodePath);
                    }
                } else {
                    this.isVisible = true;
                }
            } 
            if(n.isNodeType(Configuration.SITEMAPITEM_MIXIN_REPOSITORYBASED)) {
                if(this.isWildCard()) {
                    if(this.getParentItem() != null && this.getParentItem().isVisible()) {
                        // set repository based and visible on true
                        this.isRepositoryBased = true;
                        this.isVisible = true;
                    }
                } else {
                    log.warn("A repositorybased mixin does only have meaning on a SiteMapItem '*'. Skipping repository based for '{}'", nodePath);
                }
            }
        } catch (RepositoryException e) {
            log.error("RepositoryException : ", e);
        }
        
        this.relativeContentPath = getValueProvider().getString(Configuration.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH);
        this.componentConfigurationId = getValueProvider().getString(Configuration.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID);
        String[] rolesProp = getValueProvider().getStrings(Configuration.SITEMAPITEM_PROPERTY_ROLES);
        if(rolesProp!=null) {
            this.roles = Arrays.asList(rolesProp);
        }
        
        this.propertyMap = getValueProvider().getPropertyMap();
        
        init(jcrNode);
    }

    private void init(Node node) {
        try{
            for(NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext();) {
                Node child = nodeIt.nextNode();
                if(child == null) {
                    log.warn("skipping null node");
                    continue;
                }
                if(child.isNodeType(Configuration.NODETYPE_HST_SITEMAPITEM)) {
                    try {
                        HstSiteMapItemService siteMapItemService = new HstSiteMapItemService(child, siteMapRootNodePath, this, this.hstSiteMap);
                        childSiteMapItems.put(siteMapItemService.getValue(), siteMapItemService);
                    } catch (ServiceException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Skipping root sitemap '{}'", child.getPath(), e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Skipping root sitemap '{}'", child.getPath());
                        }
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("Skipping node '{}' because is not of type '{}'", child.getPath(), Configuration.NODETYPE_HST_SITEMAPITEM);
                    }
                }
            } 
        } catch (RepositoryException e) {
            log.warn("Skipping SiteMap structure due to Repository Exception ", e);
        }
    }
    
    public Service[] getChildServices() {
        return childSiteMapItems.values().toArray(new Service[childSiteMapItems.size()]);
    }

    public HstSiteMapItem getChild(String value) {
        return this.childSiteMapItems.get(value);
    }

    
    
    public List<HstSiteMapItem> getChildren() {
        return new ArrayList<HstSiteMapItem>(this.childSiteMapItems.values());
    }

    public String getComponentConfigurationId() {
        return this.componentConfigurationId;
    }

    public String getId() {
        return this.id;
    }

    public String getPath() {
        return this.path;
    }

    public Map<String, Object> getProperties() {
        return this.propertyMap.getAllMapsCombined();
    }

    public String getRelativeContentPath() {
        return this.relativeContentPath;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    public String getValue() {
        return this.value;
    }
    
    public boolean isWildCard() {
        return this.isWildCard;
    }
    
    public boolean isAny() {
        return this.isAny;
    }
    
    public boolean isRepositoryBased() {
        return isRepositoryBased;
    }

    public boolean isVisible() {
        return isVisible;
    }
    
    public HstSiteMap getHstSiteMap() {
        return this.hstSiteMap;
    }

    public HstSiteMapItem getParentItem() {
        return this.parentItem;
    }
    
    public String getParameterizedPath(){
        return this.parameterizedPath;
    }
    
    public int getWildCardAnyOccurences(){
        return this.occurences;
    }

   
    // ---- BELOW FOR INTERNAL CORE SITEMAP MAP RESOLVING && LINKREWRITING ONLY  
    
    public void addWildCardPrefixedChildSiteMapItems(HstSiteMapItemService hstSiteMapItem){
        this.containsWildCardChildSiteMapItems.add(hstSiteMapItem);
    }
    
    public void addAnyPrefixedChildSiteMapItems(HstSiteMapItemService hstSiteMapItem){
        this.containsAnyChildSiteMapItems.add(hstSiteMapItem);
    }
    
    
    public HstSiteMapItem getWildCardPatternChild(String value, List<HstSiteMapItem> excludeList){
        if(value == null || containsWildCardChildSiteMapItems.isEmpty()) {
            return null;
        }
        return match(value, containsWildCardChildSiteMapItems, excludeList);
    }
    
    public HstSiteMapItem getAnyPatternChild(String[] elements, int position, List<HstSiteMapItem> excludeList){
        if(value == null || containsAnyChildSiteMapItems.isEmpty()) {
            return null;
        }
        StringBuffer remainder = new StringBuffer(elements[position]);
        while(++position < elements.length) {
            remainder.append("/").append(elements[position]);
        }
        return match(remainder.toString(), containsAnyChildSiteMapItems, excludeList);
    }
    
    
    private HstSiteMapItem match(String value, List<HstSiteMapItemService> patternSiteMapItems, List<HstSiteMapItem> excludeList) {
        
        for(HstSiteMapItemService item : patternSiteMapItems){
            // if in exclude list, go to next
            if(excludeList.contains(item)) {
                continue;
            }
            // postFix must match
            String itemPrefix = item.getPrefix();
            if(itemPrefix != null && !"".equals(itemPrefix)){
                if(itemPrefix.length() >= value.length()) {
                    // can never match
                    continue;
                }
                if(!value.substring(0, itemPrefix.length()).equals(itemPrefix)){
                    // wildcard prefixed sitemap does not match the prefix. we can stop
                    continue;
                }
            }
            
            String itemPostfix = item.getPostfix();
            if(itemPostfix != null && !"".equals(itemPostfix)){
                if(itemPostfix.length() >= value.length()) {
                    // can never match
                    continue;
                }
                if(!value.substring(value.length() - itemPostfix.length()).equals(itemPostfix)){
                    // wildcard prefixed sitemap does not match the postfix . we can stop
                    continue;
                }
            }
            // if we got here, we passed the prefix and postfix test: return this item
            return item;
        }
        return null;
    }

    public String getPostfix(){
        return this.postfix;
    }
    
    public String getPrefix(){
        return this.prefix;
    }
    
    public boolean containsWildCard() {
        return this.containsWildCard;
    }
    
    public boolean containsAny() {
        return this.containsAny;
    }

}
