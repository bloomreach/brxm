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
package org.hippoecm.hst.core.linking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.map.LRUMap;
import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItemService;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BasicLocationMapTree implements LocationMapTree{

    private final static Logger log = LoggerFactory.getLogger(BasicLocationMapTree.class);
    private final static int DEFAULT_CACHE_SIZE = 500;
    
    private Map<String, LocationMapTreeItem> children = new HashMap<String, LocationMapTreeItem>();
    private String canonicalSiteContentPath;
    private Map<String, ResolvedLocationMapTreeItem> cache;
    
    @SuppressWarnings("unchecked")
    public BasicLocationMapTree(String canonicalSiteContentPath) {
        this.cache = Collections.synchronizedMap(new LRUMap(DEFAULT_CACHE_SIZE));
        this.canonicalSiteContentPath = canonicalSiteContentPath;
        
    }
    
    public void add(String unresolvedPath, HstSiteMapItem hstSiteMapItem){
        Properties params = new Properties();
        // see if there are any SiteMapItems which are wildcard or any matchers and set these as properties.
        
        // list of the sitemap items ancestors + the sitemap item itself. The last item in the list is the top parent
        List<HstSiteMapItem> ancestorItems = new ArrayList<HstSiteMapItem>();
        ancestorItems.add(hstSiteMapItem);
        HstSiteMapItem parent = hstSiteMapItem.getParentItem();
        while(parent != null) {
            ancestorItems.add(parent);
            parent = parent.getParentItem();
        }
        
        // traverse the ancestors list now to see if there are wildcard or any matchers
        int index = ancestorItems.size();
        while(index-- != 0) {
            HstSiteMapItemService s = (HstSiteMapItemService)ancestorItems.get(index);
            if(s.isWildCard()) {
                params.put(String.valueOf(params.size()+1), Configuration.WILDCARD);
            } else if(s.isAny()) {
                params.put(String.valueOf(params.size()+1), Configuration.ANY);
            } else if( s.containsWildCard() ) {
                // we assume a postfix containing a "." only meant for document url extension, disregard for linkmatching first
                String paramVal = s.getPrefix()+Configuration.WILDCARD;
                if(s.getPostfix().indexOf(".") > -1) {
                    String post = s.getPostfix().substring(0,s.getPostfix().indexOf("."));
                    if(!"".equals(post)) {
                        paramVal += post;
                    }
                } else {
                    paramVal += s.getPostfix();
                }
                params.put(String.valueOf(params.size()+1), paramVal);
            } else if( s.containsAny() ) {
               // we assume a postfix containing a "." only meant for document url extension, disregard for linkmatching first
                String paramVal = s.getPrefix()+Configuration.ANY;
                if(s.getPostfix().indexOf(".") > -1) {
                    String post = s.getPostfix().substring(0,s.getPostfix().indexOf("."));
                    if(!"".equals(post)) {
                        paramVal += post;
                    }
                } else {
                    paramVal += s.getPostfix();
                }
                params.put(String.valueOf(params.size()+1), paramVal);
            }
        }
        
        PropertyParser pp = new PropertyParser(params);
        String resolvedPath = (String)pp.resolveProperty("relative contentpah", unresolvedPath);
        
        if(resolvedPath == null) {
            log.warn("Unable to resolve relative content path : '{}'. We skip this path for linkrewriting", unresolvedPath);
            return;
        } 
        log.debug("Translated relative contentpath '{}' --> '{}'", unresolvedPath, resolvedPath);
        List<String> pathFragment = new ArrayList<String>(Arrays.asList(resolvedPath.split("/")));
        
        add(pathFragment, hstSiteMapItem);
    }
    
    public void add(List<String> pathFragment, HstSiteMapItem hstSiteMapItem){
        
        BasicLocationMapTreeItem child = (BasicLocationMapTreeItem) getTreeItem(pathFragment.get(0));
        if(child == null) {
            child = new BasicLocationMapTreeItem();
            this.children.put(pathFragment.get(0), child);
        }
        pathFragment.remove(0);
        child.add(pathFragment , hstSiteMapItem);
    }
    
    public LocationMapTreeItem getTreeItem(String name) {
        return children.get(name);
    }

    
    public ResolvedLocationMapTreeItem match(String path, HstSite hstSite,boolean representsDocument) {
        String origPath = path;
      
        if(!path.startsWith(this.getCanonicalSiteContentPath())){
            log.debug("Cannot convert path '{}' for hstSite '{}' because the path does not start " +
                    "with the content path of the hstSite. Return null", path, hstSite.getName());
            return null;
        }
        path = path.substring(this.getCanonicalSiteContentPath().length());
        // normalize leading and trailing slashes
        path = PathUtils.normalizePath(path);
        
        ResolvedLocationMapTreeItem o = this.cache.get(path);
        
        if(o != null) {
            if(o instanceof NullResolvedLocationMapTreeItem) {
                return null;
            }
            return o;
        }
        
        String[] elements = path.split("/"); 
        
        /*
         * The catch all sitemap item in case none matches (might be the sitemap item that delivers a 404)
         */
        LocationMapTreeItem locationSiteMapTreeItemAny = this.getTreeItem(Configuration.ANY);
        
        LocationMapTreeItem locationMapTreeItem = this.getTreeItem(elements[0]);
        Properties params = new Properties();
        
        PropertyParser pp = new PropertyParser(params);
        
        if(locationMapTreeItem == null) {
            // check for a wildcard matcher first:
            log.debug("Did not find a 'root location' for '{}'. Try to find a wildcard matching location item", elements[0]);
            locationMapTreeItem = this.getTreeItem(Configuration.WILDCARD);
            if(locationMapTreeItem == null) {
                if(locationSiteMapTreeItemAny == null) {
                    log.warn("Did not find a matching sitemap item and there is no catch all sitemap item configured (the ** matcher directly under the sitemap node). Return null");
                    cache.put(path, new NullResolvedLocationMapTreeItem());
                    return null;
                } else {
                    log.warn("Did not find a matching sitemap item at all. Return the catch all sitemap item (the ** matcher) ");
                    // The ** has the value of the entire pathInfo
                    
                    params.put(String.valueOf(params.size()+1), path);
                    if(locationSiteMapTreeItemAny.getHstSiteMapItems().size() > 0) {
                        // take the first one if there are more matching sitemap items
                        if(locationSiteMapTreeItemAny.getHstSiteMapItems().size() > 1) {
                            log.debug("Multiple sitemap items are suited equally for linkrewrite of '{}'. We Take the first.", path);
                        }
                        HstSiteMapItem hstSiteMapItem = locationSiteMapTreeItemAny.getHstSiteMapItems().get(0);
                        String resolvedPath = (String)pp.resolveProperty("parameterizedPath", ((HstSiteMapItemService)hstSiteMapItem).getParameterizedPath());
                        if(resolvedPath == null) {
                            log.warn("Unable to resolve '{}'", ((HstSiteMapItemService)hstSiteMapItem).getParameterizedPath());
                        }
                        ResolvedLocationMapTreeItem r = new ResolvedLocationMapTreeItemImpl(resolvedPath, hstSiteMapItem.getId());
                        this.cache.put(path, r);
                        return r;
                    }
                }
            } else {
                    params.put(String.valueOf(params.size()+1), elements[0]);
            }
        }
        
        
        LocationMapTreeItem matchedLocationMapTreeItem =  resolveMatchingSiteMap(locationMapTreeItem, params, 1, elements);
        if(matchedLocationMapTreeItem == null || matchedLocationMapTreeItem.getHstSiteMapItems().size() == 0) {
            log.warn("Unable to linkrewrite '{}' to any sitemap item", origPath);
            cache.put(path, new NullResolvedLocationMapTreeItem());
            return null;
        }
        HstSiteMapItem hstSiteMapItem = null;
      
        if(matchedLocationMapTreeItem.getHstSiteMapItems().size() > 1) {
            log.debug("Multiple sitemap items are suited equally for linkrewrite of '{}'. If we represent a document, see if can map to an extension", path);
            for(HstSiteMapItem item : matchedLocationMapTreeItem.getHstSiteMapItems()) {
                HstSiteMapItemService serv = (HstSiteMapItemService)item;
                if(representsDocument) {
                    if(serv.getExtension() != null) {
                        //  found a sitemap item with an extension! Take this one
                        hstSiteMapItem = serv;
                        break;
                    } else {
                        continue;
                    }
                } else {
                    if(serv.getExtension() == null) {
                        //  found a sitemap item without an extension! Take this one
                        hstSiteMapItem = serv;
                        break;
                    } else {
                        continue;
                    }
                }
            }
            if(hstSiteMapItem == null) {
                log.debug("Did not find a sitemap item that can represent this item. We return the first sitemap item.");
            }
        } 
        
        if(hstSiteMapItem == null){
            hstSiteMapItem = matchedLocationMapTreeItem.getHstSiteMapItems().get(0);
        }
        
        String resolvedPath = (String)pp.resolveProperty("parameterizedPath", ((HstSiteMapItemService)hstSiteMapItem).getParameterizedPath());
        if(resolvedPath == null) {
            log.warn("Unable to resolve '{}'", ((HstSiteMapItemService)hstSiteMapItem).getParameterizedPath());
        }
        
        log.info("Succesfully rewrote path '{}' into new sitemap path '{}'", origPath, resolvedPath);
        
        ResolvedLocationMapTreeItem r = new ResolvedLocationMapTreeItemImpl(resolvedPath, hstSiteMapItem.getId());
        
        
        cache.put(path, r);
        return r;
      
    }

    
    
    private LocationMapTreeItem resolveMatchingSiteMap(LocationMapTreeItem locationMapTreeItem, Properties params, int position, String[] elements) {
        return traverseInToLocationMapTreeItem(locationMapTreeItem, params, position, elements, new ArrayList<LocationMapTreeItem>());
     }

     private LocationMapTreeItem traverseInToLocationMapTreeItem(LocationMapTreeItem locationMapTreeItem, Properties params, int position, String[] elements, List<LocationMapTreeItem> checkedLocationMapTreeItems) {
         
         checkedLocationMapTreeItems.add(locationMapTreeItem);
         if(position == elements.length) {
            // we are ready
            return locationMapTreeItem;
        }
        if(locationMapTreeItem.getChild(elements[position]) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(elements[position]))) {
            return traverseInToLocationMapTreeItem(locationMapTreeItem.getChild(elements[position]), params, ++position, elements, checkedLocationMapTreeItems);
        } else if(locationMapTreeItem.getChild(Configuration.WILDCARD) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(Configuration.WILDCARD))) {
            params.put(String.valueOf(params.size()+1), elements[position]);
            return traverseInToLocationMapTreeItem(locationMapTreeItem.getChild(Configuration.WILDCARD), params, ++position, elements, checkedLocationMapTreeItems);
        } else if(locationMapTreeItem.getChild(Configuration.ANY) != null ) {
            return getANYMatchingLocationMapTreeItem(locationMapTreeItem, params,position, elements);
        }  
        else {
            // We did not find a match for traversing this sitemap item tree. Traverse up, and try another tree
            return traverseUp(locationMapTreeItem, params, position, elements, checkedLocationMapTreeItems);
        }
        
     }


     private LocationMapTreeItem traverseUp(LocationMapTreeItem locationMapTreeItem, Properties params, int position, String[] elements, List<LocationMapTreeItem> checkedLocationMapTreeItems) {
        if(locationMapTreeItem == null) {
            return null;
        }
        if(((BasicLocationMapTreeItem)locationMapTreeItem).isWildCard()) {
            // as this tree path did not result in a match, remove some params again
            if(locationMapTreeItem.getChild(Configuration.WILDCARD) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(Configuration.WILDCARD))){
                return traverseInToLocationMapTreeItem(locationMapTreeItem, params, position, elements, checkedLocationMapTreeItems);
            } else if(locationMapTreeItem.getChild(Configuration.ANY) != null) {
                return traverseInToLocationMapTreeItem(locationMapTreeItem, params,position, elements, checkedLocationMapTreeItems);
            }
            params.remove(String.valueOf(params.size()));
            return traverseUp(locationMapTreeItem.getParentItem(),params, --position, elements, checkedLocationMapTreeItems );
        } else if(locationMapTreeItem.getChild(Configuration.WILDCARD) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(Configuration.WILDCARD))){
            return traverseInToLocationMapTreeItem(locationMapTreeItem, params, position, elements, checkedLocationMapTreeItems);
        } else if(locationMapTreeItem.getChild(Configuration.ANY) != null ){
            return traverseInToLocationMapTreeItem(locationMapTreeItem, params,position, elements, checkedLocationMapTreeItems);
        } else {    
            return traverseUp(locationMapTreeItem.getParentItem(),params, --position, elements, checkedLocationMapTreeItems );
        }
        
     }
     
     private LocationMapTreeItem getANYMatchingLocationMapTreeItem(LocationMapTreeItem locationMapTreeItem, Properties params, int position,
             String[] elements) {
             StringBuffer remainder = new StringBuffer(elements[position]);
             while(++position < elements.length) {
                 remainder.append("/").append(elements[position]);
             }
             params.put(String.valueOf(params.size()+1), remainder.toString());
             return locationMapTreeItem.getChild(Configuration.ANY);
     }
    
    public String getCanonicalSiteContentPath() {
      return this.canonicalSiteContentPath;
    }
    
    
    /*
     * To be able to cache 'null', we use this placeholder null impl 
     */
    private class NullResolvedLocationMapTreeItem  implements ResolvedLocationMapTreeItem{

        private static final long serialVersionUID = 1L;

        public String getHstSiteMapItemId() {
            return null;
        }

        public String getPath() {
            return null;
        }
        
    }

}
