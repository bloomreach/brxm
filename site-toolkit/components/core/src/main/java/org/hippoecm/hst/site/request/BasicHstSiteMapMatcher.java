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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.map.LRUMap;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItemService;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHstSiteMapMatcher implements HstSiteMapMatcher{
    
    private final static Logger log = LoggerFactory.getLogger(BasicHstSiteMapMatcher.class);
    
    // the equivalence for *
    public final static String WILDCARD = "_default_";
    
    // the equivalence for **
    public final static String ANY = "_any_";
    
    /*
     * Global cached map for *all subsites*
     */
    private Map<String, ResolvedSiteMapItem> cache = Collections.synchronizedMap(new LRUMap(10000));
    
    public void invalidate(){
        this.cache.clear();
    }
    
    public ResolvedSiteMapItem match(String pathInfo, HstSite hstSite) {
        String key = hstSite.getContentPath() + "_" + pathInfo;
        ResolvedSiteMapItem cached = cache.get(key);
        if(cached != null) {
            if(cached instanceof NullResolvedSiteMapItem) {
                log.warn("For path '{}' no sitemap item can be matched", key);
                return null;
            }
            return cached;
        }
        
        Properties params = new Properties();
        
        pathInfo = PathUtils.normalizePath(pathInfo);
        String[] elements = pathInfo.split("/"); 
        
        /*
         * The catch all sitemap item in case none matches (might be the sitemap item that delivers a 404)
         */
        HstSiteMapItem hstSiteMapItemAny = hstSite.getSiteMap().getSiteMapItem(ANY);
        
        
        HstSiteMapItem hstSiteMapItem = hstSite.getSiteMap().getSiteMapItem(elements[0]);
        
        if(hstSiteMapItem == null) {
            // check for a wildcard matcher first:
            log.debug("Did not find a 'root sitemap item' for '{}'. Try to find a wildcard matching sitemap", elements[0]);
            hstSiteMapItem = hstSite.getSiteMap().getSiteMapItem(WILDCARD);
            if(hstSiteMapItem == null) {
                if(hstSiteMapItemAny == null) {
                    log.warn("Did not find a matching sitemap item and there is no catch all sitemap item configured (the ** matcher directly under the sitemap node). Return null");
                    cache.put(key, new NullResolvedSiteMapItem());
                    return null;
                } else {
                    log.warn("Did not find a matching sitemap item at all. Return the catch all sitemap item (the ** matcher) ");
                    // The ** has the value of the entire pathInfo
                    params.put("1", pathInfo);
                    ResolvedSiteMapItem r = new ResolvedSiteMapItemImpl(hstSiteMapItemAny, params, pathInfo);
                    cache.put(key, r);
                    return r;
                }
            } else {
                params.put(String.valueOf(params.size()+1), elements[0]);
            }
        }
        
       
        HstSiteMapItem matchedSiteMapItem =  resolveMatchingSiteMap(hstSiteMapItem, params, 1, elements);
      
        if(matchedSiteMapItem == null) {
            log.warn("No matching sitemap item found for path '{}'. Cannot return ResolvedSiteMapItem. Return null", pathInfo);
            cache.put(key, new NullResolvedSiteMapItem());
            return null;
        }
        
        if(log.isInfoEnabled()){
            String path = matchedSiteMapItem.getId();
            path = path.replace("_default_", "*");
            path = path.replace("_any_", "**");
            log.info("For path '{}' we found SiteMapItem with path '{}'", pathInfo, path);
            log.debug("Params for resolved sitemap item: '{}'", params);
        }
        
        ResolvedSiteMapItem r = new ResolvedSiteMapItemImpl(matchedSiteMapItem, params, pathInfo);
        cache.put(key, r);
        return r;
    
    }

    private HstSiteMapItem resolveMatchingSiteMap(HstSiteMapItem hstSiteMapItem, Properties params, int position, String[] elements) {
       return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, new ArrayList<HstSiteMapItem>());
    }

    private HstSiteMapItem traverseInToSiteMapItem(HstSiteMapItem hstSiteMapItem, Properties params, int position, String[] elements, List<HstSiteMapItem> checkedSiteMapItems) {
        HstSiteMapItemService hstSiteMapItemService = (HstSiteMapItemService)hstSiteMapItem;
        
        checkedSiteMapItems.add(hstSiteMapItemService);
        if(position == elements.length) {
           // we are ready
           return hstSiteMapItemService;
       }
       HstSiteMapItem s; 
       if( (s = hstSiteMapItemService.getChild(elements[position])) != null && !checkedSiteMapItems.contains(s)) {
           return traverseInToSiteMapItem(s, params, ++position, elements, checkedSiteMapItems);
       } else if( (s = hstSiteMapItemService.getWildCardPatternChild(elements[position], checkedSiteMapItems)) != null ) {
           String parameter = getStrippedParameter((HstSiteMapItemService)s, elements[position]);
           params.put(String.valueOf(params.size()+1), parameter);
           return traverseInToSiteMapItem(s, params, ++position, elements, checkedSiteMapItems);
       } else if( (s = hstSiteMapItemService.getChild(WILDCARD)) != null && !checkedSiteMapItems.contains(s)) {
           params.put(String.valueOf(params.size()+1), elements[position]);
           return traverseInToSiteMapItem(s, params, ++position, elements, checkedSiteMapItems);
       } else if( (s = hstSiteMapItemService.getAnyPatternChild(elements, position, checkedSiteMapItems)) != null ) {
           StringBuffer remainder = new StringBuffer(elements[position]);
           while(++position < elements.length) {
               remainder.append("/").append(elements[position]);
           }
           String parameter = getStrippedParameter((HstSiteMapItemService)s, remainder.toString());
           params.put(String.valueOf(params.size()+1), parameter);
           return s;
       } 
       else if(hstSiteMapItemService.getChild(ANY) != null ) {
           StringBuffer remainder = new StringBuffer(elements[position]);
           while(++position < elements.length) {
               remainder.append("/").append(elements[position]);
           }
           params.put(String.valueOf(params.size()+1), remainder.toString());
           return hstSiteMapItem.getChild(ANY);
       }  
       else {
           // We did not find a match for traversing this sitemap item tree. Traverse up, and try another tree
           return traverseUp(hstSiteMapItemService, params, position, elements, checkedSiteMapItems);
       }
       
    }

    private HstSiteMapItem traverseUp(HstSiteMapItem hstSiteMapItem, Properties params, int position, String[] elements, List<HstSiteMapItem> checkedSiteMapItems) {
       if(hstSiteMapItem == null) {
           return null;
       }
       HstSiteMapItem s; 
       if(hstSiteMapItem.isWildCard()) {
           // as this tree path did not result in a match, remove some params again
           if( (s = hstSiteMapItem.getChild(WILDCARD)) != null && !checkedSiteMapItems.contains(s)){
               return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
           } else if(hstSiteMapItem.getChild(ANY) != null) {
               return traverseInToSiteMapItem(hstSiteMapItem, params,position, elements, checkedSiteMapItems);
           }
           params.remove(String.valueOf(params.size()));
           return traverseUp(hstSiteMapItem.getParentItem(),params, --position, elements, checkedSiteMapItems );
       } else if( (s = hstSiteMapItem.getChild(WILDCARD)) != null && !checkedSiteMapItems.contains(s)){
           return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
       } else if(hstSiteMapItem.getChild(ANY) != null ){
           return traverseInToSiteMapItem(hstSiteMapItem, params,position, elements, checkedSiteMapItems);
       } else {    
           return traverseUp(hstSiteMapItem.getParentItem(),params, --position, elements, checkedSiteMapItems );
       }

    }
    
    private String getStrippedParameter(HstSiteMapItemService s, String parameter) {
        String removePrefix = ((HstSiteMapItemService)s).getPrefix();
        String removePostfix = ((HstSiteMapItemService)s).getPostfix();
        if(removePrefix != null && parameter.startsWith(removePrefix))  {
           parameter = parameter.substring(removePrefix.length());
        }
        if(removePostfix != null && parameter.endsWith(removePostfix))  {
           parameter = parameter.substring(0, (parameter.length() - removePostfix.length()));
        }
        return parameter;
    }
    
    
    /*
     * Placeholder for a null cached version
     */
    private class NullResolvedSiteMapItem implements ResolvedSiteMapItem{

        public HstComponentConfiguration getHstComponentConfiguration() {
            return null;
        }

        public HstSiteMapItem getHstSiteMapItem() {
            return null;
        }

        public String getParameter(String name) {
            return null;
        }

        public Properties getParameters() {
            return null;
        }

        public String getRelativeContentPath() {
            return null;
        }

        public HstSiteMenus getSiteMenus() {
            return null;
        }
        
        public int getStatusCode(){
            return 0;
        }

        public int getErrorCode() {
            return 0;
        }

        
        public String getPathInfo() {
            return null;
        }

        
    }
}
