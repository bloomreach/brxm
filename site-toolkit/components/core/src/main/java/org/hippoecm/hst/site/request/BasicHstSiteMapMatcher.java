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
package org.hippoecm.hst.site.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hippoecm.hst.configuration.hosting.NotFoundException;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItemService;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkImpl;
import org.hippoecm.hst.core.linking.HstLinkProcessor;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BasicHstSiteMapMatcher
 * 
 * @version $Id$
 */
public class BasicHstSiteMapMatcher implements HstSiteMapMatcher{
    
    private final static Logger log = LoggerFactory.getLogger(BasicHstSiteMapMatcher.class);
    
    // the equivalence for *
    public final static String WILDCARD = "_default_";
    
    // the equivalence for **
    public final static String ANY = "_any_";
     
    private HstLinkProcessor linkProcessor;
    
    public void setLinkProcessor(HstLinkProcessor linkProcessor) {
        this.linkProcessor = linkProcessor;
    }

    
    public ResolvedSiteMapItem match(String pathInfo, ResolvedMount resolvedMount) throws NotFoundException {
        HstSite hstSite = resolvedMount.getMount().getHstSite();
       
        Properties params = new Properties();
        
        pathInfo = PathUtils.normalizePath(pathInfo);
        
        if(linkProcessor != null) {
            HstLink link = new HstLinkImpl(pathInfo, resolvedMount.getMount(), false, false);
            link = linkProcessor.preProcess(link);
            pathInfo = link.getPath();
        }
        
        String[] elements = pathInfo.split("/");

        final HstSiteMap siteMap = hstSite.getSiteMap();
        HstSiteMapItem hstSiteMapItem = siteMap.getSiteMapItem(elements[0]);
        
        HstSiteMapItem matchedSiteMapItem = null;
        if(hstSiteMapItem != null) {
            matchedSiteMapItem =  resolveMatchingSiteMap(hstSiteMapItem, params, 1, elements);
        }
        
        // still no match, try if there are root components like *.xxx that match
        if(matchedSiteMapItem == null) {
            params.clear();
            // check for partial wildcard (*.xxx) matcher first
            for(HstSiteMapItem item : siteMap.getSiteMapItems()) {
                HstSiteMapItemService service = (HstSiteMapItemService)item;
                if(service.containsWildCard() && service.patternMatch(elements[0], service.getPrefix(), service.getPostfix())) {
                    String parameter = getStrippedParameter((HstSiteMapItemService)service, elements[0]);
                    params.put(String.valueOf(params.size()+1), parameter);
                    matchedSiteMapItem =  resolveMatchingSiteMap(service, params, 1, elements);
                    if(matchedSiteMapItem != null) {
                        // we have a matching sitemap item.
                        break;
                    }
                }
            }
        }
        
        // still no match, try if there is root components that is *
        if(matchedSiteMapItem == null) {
            params.clear();
            // check for a wildcard (*) matcher :
            hstSiteMapItem = siteMap.getSiteMapItem(WILDCARD);
            if(hstSiteMapItem != null) {
                params.put(String.valueOf(params.size()+1), elements[0]);
                matchedSiteMapItem =  resolveMatchingSiteMap(hstSiteMapItem, params, 1, elements);
            }
        }
        
        // still no match, try if there are root components like **.xxx that match
        if(matchedSiteMapItem == null) {
            params.clear();
         // check for partial wildcard (**.xxx) matcher first
            for(HstSiteMapItem item : siteMap.getSiteMapItems()) {
                HstSiteMapItemService service = (HstSiteMapItemService)item;
                if(service.containsAny() && service.patternMatch(pathInfo, service.getPrefix(), service.getPostfix())) {
                    String parameter = getStrippedParameter((HstSiteMapItemService)service, pathInfo);
                    params.put(String.valueOf(params.size()+1), parameter);
                    matchedSiteMapItem = item;
                    // we have a matching sitemap item.
                    break;
                }
            }
        }
        
        // still no match, try if there is root components that is **
        if(matchedSiteMapItem == null) {
            params.clear();
            // check for a wildcard (**) matcher :
            HstSiteMapItem hstSiteMapItemAny = siteMap.getSiteMapItem(ANY);
            if(hstSiteMapItemAny == null) {
                log.info("Did not find a matching sitemap item for path '{}', Mount '{}' and Host '"+resolvedMount.getResolvedVirtualHost().getResolvedHostName()+"'" +
                        ". Return null", pathInfo, resolvedMount.getMount().getParent() == null ? "hst:root" : resolvedMount.getMount().getMountPath() );
                throw new NotFoundException("PathInfo '"+pathInfo+"' could not be matched");
            } else {
                // The ** has the value of the entire pathInfo
                params.put(String.valueOf(params.size()+1), pathInfo);
                matchedSiteMapItem = hstSiteMapItemAny;
            }
            
        }
        
        if(log.isInfoEnabled()){
            String path = matchedSiteMapItem.getId();
            path = path.replace("_default_", "*");
            path = path.replace("_any_", "**");
            log.info("For path '{}' we found SiteMapItem with path '{}'", pathInfo, path);
            log.debug("Params for resolved sitemap item: '{}'", params);
        }
        
        ResolvedSiteMapItem r = new ResolvedSiteMapItemImpl(matchedSiteMapItem, params, pathInfo, resolvedMount);
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
           if (s.isAny() || s.isWildCard()) {
               // this can happen when the pathInfo to match contains _default_ or _any_  : It is a corner case
               params.put(String.valueOf(params.size()+1), getStrippedParameter((HstSiteMapItemService)s, elements[position]));
           }
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
        HstSiteMapItemService hstSiteMapItemService = (HstSiteMapItemService)hstSiteMapItem;
        if(hstSiteMapItem == null) {
           return null;
       }
       HstSiteMapItem s; 
       if(hstSiteMapItem.isWildCard()) {
           if( (s = hstSiteMapItem.getChild(WILDCARD)) != null && !checkedSiteMapItems.contains(s)){
               return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
           } else if( (s = hstSiteMapItemService.getWildCardPatternChild(elements[position], checkedSiteMapItems)) != null && !checkedSiteMapItems.contains(s)) {
               return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
           }else if(hstSiteMapItem.getChild(ANY) != null) {
               return traverseInToSiteMapItem(hstSiteMapItem, params,position, elements, checkedSiteMapItems);
           } else if( (s = hstSiteMapItemService.getAnyPatternChild(elements, position, checkedSiteMapItems)) != null && !checkedSiteMapItems.contains(s)) {
               return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
           } 
           // as this tree path did not result in a match, remove some params again
           params.remove(String.valueOf(params.size()));
           return traverseUp(hstSiteMapItem.getParentItem(),params, --position, elements, checkedSiteMapItems );
       } else if( (s = hstSiteMapItem.getChild(WILDCARD)) != null && !checkedSiteMapItems.contains(s)){
           return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
       } else if( (s = hstSiteMapItemService.getWildCardPatternChild(elements[position], checkedSiteMapItems)) != null && !checkedSiteMapItems.contains(s)) {
            return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
       } else if(hstSiteMapItem.getChild(ANY) != null ){
           return traverseInToSiteMapItem(hstSiteMapItem, params,position, elements, checkedSiteMapItems);
       } else if( (s = hstSiteMapItemService.getAnyPatternChild(elements, position, checkedSiteMapItems)) != null && !checkedSiteMapItems.contains(s)) {
           return traverseInToSiteMapItem(hstSiteMapItem, params, position, elements, checkedSiteMapItems);
       } else {    
           return traverseUp(hstSiteMapItem.getParentItem(),params, --position, elements, checkedSiteMapItems );
       }

    }
    
    private String getStrippedParameter(HstSiteMapItemService s, String parameter) {
        String removePrefix = s.getPrefix();
        String removePostfix = s.getPostfix();
        if(removePrefix != null && parameter.startsWith(removePrefix))  {
           parameter = parameter.substring(removePrefix.length());
        }
        if(removePostfix != null && parameter.endsWith(removePostfix))  {
           parameter = parameter.substring(0, (parameter.length() - removePostfix.length()));
        }
        return parameter;
    }
    

    public void invalidate() {
        // currently nothing to invalidate
    }
}
