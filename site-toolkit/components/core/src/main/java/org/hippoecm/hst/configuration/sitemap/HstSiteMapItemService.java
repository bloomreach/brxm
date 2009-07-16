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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
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

    private int statusCode; 
    
    private int errorCode; 
    
    private String parameterizedPath;
    
    private int occurences;
    
    private String relativeContentPath;
    
    private String componentConfigurationId;
    
    private String portletComponentConfigurationId;
    
    private List<String> roles;

    private boolean isWildCard;
    
    private boolean isAny;
    
    private HstSiteMap hstSiteMap;
    
    private HstSiteMapItemService parentItem;
    
    private Map<String,String> parameters = new HashMap<String,String>();
    
    private List<HstSiteMapItemService> containsWildCardChildSiteMapItems = new ArrayList<HstSiteMapItemService>();
    private List<HstSiteMapItemService> containsAnyChildSiteMapItems = new ArrayList<HstSiteMapItemService>();
    private boolean containsAny;
    private boolean containsWildCard;
    private String postfix; 
    private String extension;
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
        this.id = nodePath.substring(siteMapRootNodePath.length()+1);
        // currently, the value is always the nodename
        this.value = getValueProvider().getName();

        this.statusCode = getValueProvider().getLong(Configuration.SITEMAPITEM_PROPERTY_STATUSCODE).intValue();
        this.errorCode = getValueProvider().getLong(Configuration.SITEMAPITEM_PROPERTY_ERRORCODE).intValue();
        
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
        if(Configuration.WILDCARD.equals(value)) {
            occurences++; 
            parameterizedPath = parameterizedPath + "${" + occurences + "}";
            this.isWildCard = true;
        } else if(Configuration.ANY.equals(value)) {
            occurences++;
            parameterizedPath = parameterizedPath + "${" + occurences + "}";
            this.isAny = true;
        } else if(value.indexOf(Configuration.WILDCARD) > -1) {
            this.containsWildCard = true;
            this.postfix = value.substring(value.indexOf(Configuration.WILDCARD) + Configuration.WILDCARD.length());
            this.prefix = value.substring(0, value.indexOf(Configuration.WILDCARD));
            if(this.postfix.indexOf(".") > -1) {
                this.extension = this.postfix.substring(this.postfix.indexOf("."));
            }
            if(parentItem != null) {
                ((HstSiteMapItemService)parentItem).addWildCardPrefixedChildSiteMapItems(this);
            }
            occurences++;
            parameterizedPath = parameterizedPath + value.replace(Configuration.WILDCARD, "${"+occurences+"}" );
        } else if(value.indexOf(Configuration.ANY) > -1) {
            this.containsAny = true;
            this.postfix = value.substring(value.indexOf(Configuration.ANY) + Configuration.ANY.length());
            if(this.postfix.indexOf(".") > -1) {
                this.extension = this.postfix.substring(this.postfix.indexOf("."));
            }
            this.prefix = value.substring(0, value.indexOf(Configuration.ANY));
            if(parentItem != null) {
                ((HstSiteMapItemService)parentItem).addAnyPrefixedChildSiteMapItems(this);
            }
            occurences++;
            parameterizedPath = parameterizedPath + value.replace(Configuration.ANY, "${"+occurences+"}" );
        }
        else {
            parameterizedPath = parameterizedPath + value;
        }
        
        String[] parameterNames = getValueProvider().getStrings(Configuration.SITEMAPITEM_PROPERTY_PARAMETER_NAMES);
        String[] parameterValues = getValueProvider().getStrings(Configuration.SITEMAPITEM_PROPERTY_PARAMETER_VALUES);
        
        if(parameterNames != null && parameterValues != null){
           if(parameterNames.length != parameterValues.length) {
               log.warn("Skipping parameters for component because they only make sense if there are equal number of names and values");
           }  else {
               for(int i = 0; i < parameterNames.length ; i++) {
                   this.parameters.put(parameterNames[i], parameterValues[i]);
               }
           }
        }
        
        this.relativeContentPath = getValueProvider().getString(Configuration.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH);
        this.componentConfigurationId = getValueProvider().getString(Configuration.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID);
        this.portletComponentConfigurationId = getValueProvider().getString(Configuration.SITEMAPITEM_PROPERTY_PORTLETCOMPONENTCONFIGURATIONID);
        String[] rolesProp = getValueProvider().getStrings(Configuration.SITEMAPITEM_PROPERTY_ROLES);
        if(rolesProp!=null) {
            this.roles = Arrays.asList(rolesProp);
        }
        
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
        return Collections.unmodifiableList(new ArrayList<HstSiteMapItem>(this.childSiteMapItems.values()));
    }

    public String getComponentConfigurationId() {
        return this.componentConfigurationId;
    }

    public String getPortletComponentConfigurationId() {
        return this.portletComponentConfigurationId;
    }

    public String getId() {
        return this.id;
    }

    public String getRelativeContentPath() {
        return this.relativeContentPath;
    }
    

    public String getParameter(String name) {
        return this.parameters.get(name);
    }
    

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(this.parameters);
    }

    public int getStatusCode() {
        return this.statusCode;
    }
    
    public int getErrorCode() {
        return this.errorCode;
    }

    public List<String> getRoles() {
        return Collections.unmodifiableList(this.roles);
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
    
    public String getExtension(){
        return this.extension;
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
