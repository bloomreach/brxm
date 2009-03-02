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
import java.util.List;
import java.util.Properties;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHstSiteMapMatcher implements HstSiteMapMatcher{
    
    private final static Logger log = LoggerFactory.getLogger(HstSiteMapMatcher.class);
    
    // the equivalence for *
    public final static String WILDCARD = "_default_";
    
    // the equivalence for **
    public final static String ANY = "_any_";
    
    public ResolvedSiteMapItem match(String pathInfo, HstSite hstSite) {
        
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
                    return null;
                } else {
                    log.warn("Did not find a matching sitemap item at all. Return the catch all sitemap item (the ** matcher) ");
                    // The ** has the value of the entire pathInfo
                    params.put("1", pathInfo);
                    return new ResolvedSiteMapItemImpl(hstSiteMapItemAny, params);
                }
            } else {
                params.put(String.valueOf(params.size()+1), elements[0]);
            }
        }
        
       
        HstSiteMapItem matchedSiteMapItem =  resolveMatchingSiteMap(hstSiteMapItem, params, 1, elements);
      
        if(matchedSiteMapItem == null) {
            log.warn("No matching sitemap item found for path '{}'. Cannot return ResolvedSiteMapItem. Return null", pathInfo);
            return null;
        }
        
        if(log.isInfoEnabled()){
            String path = matchedSiteMapItem.getPath();
            path = path.replace("_default_", "*");
            path = path.replace("_any_", "**");
            log.info("For path '{}' we found SiteMapItem with path '{}'", pathInfo, path);
            log.debug("Params for resolved sitemap item: '{}'", params);
        }
        
        return new ResolvedSiteMapItemImpl(matchedSiteMapItem, params);
    
    }

    private HstSiteMapItem resolveMatchingSiteMap(HstSiteMapItem hstSiteMapItem, Properties params, int position, String[] elements) {
       return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, new ArrayList<HstSiteMapItem>());
    }

    private HstSiteMapItem traverseInToSiteMapItem(HstSiteMapItem hstSiteMapItem, Properties params, int position, String[] elements, List<HstSiteMapItem> checkedSiteMapItems) {
        
        checkedSiteMapItems.add(hstSiteMapItem);
        if(position == elements.length) {
           // we are ready
           return hstSiteMapItem;
       }
       if(hstSiteMapItem.getChild(elements[position]) != null && !checkedSiteMapItems.contains(hstSiteMapItem.getChild(elements[position]))) {
           return traverseInToSiteMapItem(hstSiteMapItem.getChild(elements[position]), params, ++position, elements, checkedSiteMapItems);
       } else if(hstSiteMapItem.getChild(WILDCARD) != null && !checkedSiteMapItems.contains(hstSiteMapItem.getChild(WILDCARD))) {
           params.put(String.valueOf(params.size()+1), elements[position]);
           return traverseInToSiteMapItem(hstSiteMapItem.getChild(WILDCARD), params, ++position, elements, checkedSiteMapItems);
       } else if(hstSiteMapItem.getChild(ANY) != null ) {
           return getANYMatchingSiteMap(hstSiteMapItem, params,position, elements);
       }  
       else {
           // We did not find a match for traversing this sitemap item tree. Traverse up, and try another tree
           return traverseUp(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
       }
       
    }


    private HstSiteMapItem traverseUp(HstSiteMapItem hstSiteMapItem, Properties params, int position, String[] elements, List<HstSiteMapItem> checkedSiteMapItems) {
       if(hstSiteMapItem == null) {
           return null;
       }
       if(hstSiteMapItem.isWildCard()) {
           // as this tree path did not result in a match, remove some params again
           if(hstSiteMapItem.getChild(WILDCARD) != null && !checkedSiteMapItems.contains(hstSiteMapItem.getChild(WILDCARD))){
               return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
           } else if(hstSiteMapItem.getChild(ANY) != null) {
               return traverseInToSiteMapItem(hstSiteMapItem, params,position, elements, checkedSiteMapItems);
           }
           params.remove(String.valueOf(params.size()));
           return traverseUp(hstSiteMapItem.getParentItem(),params, --position, elements, checkedSiteMapItems );
       } else if(hstSiteMapItem.getChild(WILDCARD) != null && !checkedSiteMapItems.contains(hstSiteMapItem.getChild(WILDCARD))){
           return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
       } else if(hstSiteMapItem.getChild(ANY) != null ){
           return traverseInToSiteMapItem(hstSiteMapItem, params,position, elements, checkedSiteMapItems);
       } else {    
           return traverseUp(hstSiteMapItem.getParentItem(),params, --position, elements, checkedSiteMapItems );
       }
       
    }
    
    
    private HstSiteMapItem getANYMatchingSiteMap(HstSiteMapItem hstSiteMapItem, Properties params, int position,
            String[] elements) {
            StringBuffer remainder = new StringBuffer(elements[position]);
            while(++position < elements.length) {
                remainder.append("/").append(elements[position]);
            }
            params.put(String.valueOf(params.size()+1), remainder.toString());
            return hstSiteMapItem.getChild(ANY);
    }
}
