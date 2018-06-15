/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.NotFoundException;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.platform.linking.HstLinkImpl;
import org.hippoecm.hst.core.linking.HstLinkProcessor;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.platform.configuration.sitemap.HstSiteMapItemService;
import org.hippoecm.hst.site.request.ResolvedSiteMapItemImpl;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.ANY;
import static org.hippoecm.hst.configuration.HstNodeTypes.INDEX;
import static org.hippoecm.hst.configuration.HstNodeTypes.WILDCARD;

/**
 * BasicHstSiteMapMatcher
 * 
 * @version $Id$
 */
public class BasicHstSiteMapMatcher implements HstSiteMapMatcher {
    
    private final static Logger log = LoggerFactory.getLogger(BasicHstSiteMapMatcher.class);

    private HstLinkProcessor linkProcessor;
    
    public void setLinkProcessor(HstLinkProcessor linkProcessor) {
        this.linkProcessor = linkProcessor;
    }

    
    public ResolvedSiteMapItem match(String pathInfo, ResolvedMount resolvedMount) throws NotFoundException {

        final Mount mount = resolvedMount.getMount();
        if (!mount.isMapped()) {
            throw new NotFoundException(String.format("Cannot match '%s' to a sitemap item for mount '%s' because the mount is not " +
                    "mapped and thus does not have an associated sitemap.", pathInfo, mount));
        }

        HstSite hstSite = mount.getHstSite();
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
        if(hstSiteMapItem != null && !hstSiteMapItem.isMarkedDeleted()) {
            matchedSiteMapItem =  resolveMatchingSiteMap(hstSiteMapItem, params, 1, elements);
        }
        
        // still no match, try if there are root components like *.xxx that match
        if(matchedSiteMapItem == null) {
            params.clear();
            // check for partial wildcard (*.xxx) matcher first
            for(HstSiteMapItem item : siteMap.getSiteMapItems()) {
                if (item.isMarkedDeleted()) {
                    continue;
                }
                HstSiteMapItemService service = (HstSiteMapItemService)item;
                if(service.containsWildCard() && service.patternMatch(elements[0], service.getPrefix(), service.getPostfix())) {
                    String parameter = getStrippedParameter(service, elements[0]);
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
            if(hstSiteMapItem != null && !hstSiteMapItem.isMarkedDeleted()) {
                params.put(String.valueOf(params.size()+1), elements[0]);
                matchedSiteMapItem =  resolveMatchingSiteMap(hstSiteMapItem, params, 1, elements);
            }
        }
        
        // still no match, try if there are root components like **.xxx that match
        if(matchedSiteMapItem == null) {
            params.clear();
         // check for partial wildcard (**.xxx) matcher first
            for(HstSiteMapItem item : siteMap.getSiteMapItems()) {
                if (item.isMarkedDeleted()) {
                    continue;
                }
                HstSiteMapItemService service = (HstSiteMapItemService)item;
                if(service.containsAny() && service.patternMatch(pathInfo, service.getPrefix(), service.getPostfix())) {
                    String parameter = getStrippedParameter(service, pathInfo);
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
            if(hstSiteMapItemAny == null || hstSiteMapItemAny.isMarkedDeleted()) {
                log.info("Did not find a matching sitemap item for path '{}', Mount '{}' and Host '"+resolvedMount.getMount().getVirtualHost().getHostName()+"'" +
                        ". Return null", pathInfo, resolvedMount.getMount().getParent() == null ? "hst:root" : resolvedMount.getMount().getMountPath() );
                throw new NotFoundException("PathInfo '"+pathInfo+"' could not be matched");
            } else {
                // The ** has the value of the entire pathInfo
                params.put(String.valueOf(params.size()+1), pathInfo);
                matchedSiteMapItem = hstSiteMapItemAny;
            }
            
        }

        // check wether there is an _index_ sitemap item:
        HstSiteMapItem index = matchedSiteMapItem.getChild(INDEX);
        if (index != null) {
            log.info("Found an '{}' sitemap item below '{}'. Check if the relative content path points to an existing folder/document.",
                    INDEX, getSiteMapItemPath(matchedSiteMapItem));
            ResolvedSiteMapItemImpl indexResolvedSiteMapItem = new ResolvedSiteMapItemImpl(index, params, pathInfo + "/" + INDEX, resolvedMount);
            if (indexResolvedSiteMapItem.getRelativeContentPath() != null) {
                // check whether the folder/document being referred to by the indexResolvedSiteMapItem exists : If so, use _index_ item as match
                String absolutePath = mount.getContentPath() + "/" + indexResolvedSiteMapItem.getRelativeContentPath();
                try {
                    if (RequestContextProvider.get() != null && RequestContextProvider.get().getSession().itemExists(absolutePath)) {
                        log.info("Use '{}' sitemap item below '{}' because content path '{}' for the '{}' item exists.",
                                INDEX, getSiteMapItemPath(matchedSiteMapItem), absolutePath, INDEX);
                        logMatchedItem(pathInfo, params, matchedSiteMapItem);
                        return indexResolvedSiteMapItem;
                    } else {
                        log.info("Don't use '{}' sitemap item below '{}' because content path '{}' for the '{}' item does NOT exist.",
                                INDEX, getSiteMapItemPath(matchedSiteMapItem), absolutePath, INDEX);
                    }
                } catch (RepositoryException e) {
                    log.warn("Unable to get JCR session needed to check existing of the document belonging to the _index_ " +
                            "sitemap item.", e);
                }
            }
        }

        logMatchedItem(pathInfo, params, matchedSiteMapItem);

        ResolvedSiteMapItem r = new ResolvedSiteMapItemImpl(matchedSiteMapItem, params, pathInfo, resolvedMount);
        return r;
    
    }

    private void logMatchedItem(final String pathInfo, final Properties params, final HstSiteMapItem matchedSiteMapItem) {
        if (log.isInfoEnabled()) {
            String path = getSiteMapItemPath(matchedSiteMapItem);
            log.info("For path '{}' we found SiteMapItem with path '{}'", pathInfo, path);
            log.debug("Params for resolved sitemap item: '{}'", params);
        }
    }

    private String getSiteMapItemPath(final HstSiteMapItem matchedSiteMapItem) {
        String path = matchedSiteMapItem.getId();
        path = path.replace(WILDCARD, "*");
        path = path.replace(ANY, "**");
        return path;
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
       if( (s = hstSiteMapItemService.getChild(elements[position])) != null && !checkedSiteMapItems.contains(s) && !s.isMarkedDeleted()) {
           if (s.isAny() || s.isWildCard()) {
               // this can happen when the pathInfo to match contains _default_ or _any_  : It is a corner case
               params.put(String.valueOf(params.size()+1), getStrippedParameter((HstSiteMapItemService)s, elements[position]));
           }
           return traverseInToSiteMapItem(s, params, ++position, elements, checkedSiteMapItems);
       } else if( (s = hstSiteMapItemService.getWildCardPatternChild(elements[position], checkedSiteMapItems)) != null  && !s.isMarkedDeleted()) {
           String parameter = getStrippedParameter((HstSiteMapItemService)s, elements[position]);
           params.put(String.valueOf(params.size()+1), parameter);
           return traverseInToSiteMapItem(s, params, ++position, elements, checkedSiteMapItems);
       } else if( (s = hstSiteMapItemService.getChild(WILDCARD)) != null && !checkedSiteMapItems.contains(s) && !s.isMarkedDeleted()) {
           params.put(String.valueOf(params.size()+1), elements[position]);
           return traverseInToSiteMapItem(s, params, ++position, elements, checkedSiteMapItems);
       } else if( (s = hstSiteMapItemService.getAnyPatternChild(elements, position, checkedSiteMapItems)) != null && !s.isMarkedDeleted()) {
           StringBuffer remainder = new StringBuffer(elements[position]);
           while(++position < elements.length) {
               remainder.append("/").append(elements[position]);
           }
           String parameter = getStrippedParameter((HstSiteMapItemService)s, remainder.toString());
           params.put(String.valueOf(params.size()+1), parameter);
           return s;
       } 
       else if(hstSiteMapItemService.getChild(ANY) != null && !hstSiteMapItemService.getChild(ANY).isMarkedDeleted()) {
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
