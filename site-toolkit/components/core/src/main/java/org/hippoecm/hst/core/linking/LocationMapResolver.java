/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItemService;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class capable of resolving a path to the best LocationTreeMapItem and create a ResolvedTreeMapItem for you
 * 
 */
public class LocationMapResolver {

    private final static Logger log = LoggerFactory.getLogger(LocationMapResolver.class);
    
    private static final String KEY_TO_PROPERTY_PREFIX = "key";

    private LocationMapTree locationMapTree; 
    private boolean representsDocument;
    private boolean canonical;
    private boolean isSubResolver;
    
    /**
     *  the resolved sitemap item of the current request. Note that this variable is allowed to be <code>null</code>
     */
    private ResolvedSiteMapItem resolvedSiteMapItem;

    private Set<LocationMapTreeItem> checkedLocationMapTreeItems = new HashSet<LocationMapTreeItem>();
    private Set<LocationMapTreeItem> globalCheckedLocationMapTreeItems = new HashSet<LocationMapTreeItem>();
    private Map<String,String> propertyPlaceHolderMap = new HashMap<String,String>();
    
    public LocationMapResolver(LocationMapTree locationMapTree) {
       this.locationMapTree = locationMapTree;
    }

    public void setRepresentsDocument(boolean representsDocument) {
       this.representsDocument = representsDocument; 
    }

    public void setCanonical(boolean canonical) {
        this.canonical = canonical;
    }

    public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
        this.resolvedSiteMapItem = resolvedSiteMapItem;
    }

    public void setSubResolver(boolean isSubResolver) {
        this.isSubResolver = isSubResolver;
    }
    
    /**
     * Tries to find the best match for the <code>path</code> within this <code>LocationMapTree</code> belonging to <code>HstSite</code>.
     * As it can easily happen that multiple <code>SiteMapItem</code>'s are suitable for to match the <code>path</code>, implementing
     * classes should try to return the 'best' match, unless <code>canonical</code> is true (then regardless the context, the same sitemap item must be returned). 
     * Typically, the 'best' match would be a match that resolves to a <code>SiteMapItem</code> containing a relative content path that is the most specific for the current path. When two relative content path match equally well, then
     * the following steps define the order in which a sitemap item is preferred. 
     * 
     * 1) one of the N sitemap items is the same as the current ctx sitemapitem, this one is taken (SAME) : break;
     * 2) if one of the N sitemap items is a *descendant* sitemap item of the current ctx sitemap item, that one is taken : break;
     * 3) Take the matched sitemap items (List) with the first common (shared) ancestor with the current ctx sitemap item : if List contains 1 item: return item: else continue;
     * 4) If (3) returns multiple matched items, return the ones that are the closest (wrt depth) to the common ancestor: if there is one, return item : else continue;
     * 5) If (4) returns 1 or more items, pick the first (we cannot distinguish better) 
     * 6) If still no best context hit found, we return the first matchingSiteMapItem in the matchingSiteMapItems list, as there they are all equally out of context
     * 
     * If <code>canonical</code> is <code>true</code> we return a <code>ResolvedLocationMapTreeItem</code> containing always the same <code>HstSiteMapItem</code>, regardless
     * the current context. This is useful if you want to add the canonical location of some webpage, which is highly appreciated by search engines, for example:
     * <link rel="canonical" href="http://www.hippoecm.org/news/2009/article.html" />. This becomes increasingly important when making use of faceted navigations where
     * the same document content can be shown on a website in many different contexts. 
     * 
     * @param path the path you want to match
     * @return the resolvedLocationMapTreeItem that contains a rewritten path and the hstSiteMapId which is the unique id of the
     * HstSiteMapItem that returned the best match. If no match can be made, <code>null</code> is returned 
     */ 
    public ResolvedLocationMapTreeItem resolve(String path) {
        long start = System.nanoTime();
        // normalize leading and trailing slashes
        path = PathUtils.normalizePath(path);
     
        
        String[] elements = path.split("/"); 
        
        LocationMapTreeItem matchedLocationMapTreeItem = null;
        LocationMapTreeItem locationMapTreeItem = locationMapTree.getTreeItem(elements[0]);
        HstSiteMapItem matchingSiteMapItem = null;
        if(locationMapTreeItem != null) {
            while(matchingSiteMapItem == null) {
                propertyPlaceHolderMap.clear();
                checkedLocationMapTreeItems.clear();
                checkedLocationMapTreeItems.addAll(globalCheckedLocationMapTreeItems);
                LocationMapTreeItem newMatched = resolveMatchingLocationMapTreeItem(locationMapTreeItem, 1, elements);
                if(newMatched == null || newMatched == matchedLocationMapTreeItem) {
                    /*
                     * we did not find a matching sitemap item not having the first entry as a wildcard or we found an item that
                     * we allready tested before
                     */ 
                    break;
                }
                globalCheckedLocationMapTreeItems.add(newMatched);
                matchedLocationMapTreeItem = newMatched;
                matchingSiteMapItem = resolveToSiteMapItem(matchedLocationMapTreeItem);
            }
        }
        
        // test for * matcher because we were not yet able to resolve to a matching sitemap
        if(matchingSiteMapItem == null) {
            locationMapTreeItem = locationMapTree.getTreeItem(HstNodeTypes.WILDCARD);
            if(locationMapTreeItem != null) {
                while(matchingSiteMapItem == null) {
                    propertyPlaceHolderMap.clear();
                    checkedLocationMapTreeItems.clear();
                    checkedLocationMapTreeItems.addAll(globalCheckedLocationMapTreeItems);
                    propertyPlaceHolderMap.put(KEY_TO_PROPERTY_PREFIX+String.valueOf(propertyPlaceHolderMap.size()+1), elements[0]);
                    LocationMapTreeItem newMatched = resolveMatchingLocationMapTreeItem(locationMapTreeItem, 1, elements);
                    /*
                     * we did not find a matching sitemap item not having the first entry as a wildcard or we found an item that
                     * we allready tested before
                     */ 
                    if(newMatched == null || newMatched == matchedLocationMapTreeItem) {
                        break;
                    }
                    globalCheckedLocationMapTreeItems.add(newMatched);
                    matchedLocationMapTreeItem = newMatched;
                    matchingSiteMapItem = resolveToSiteMapItem(matchedLocationMapTreeItem);
                }
            }
        }
        
       // test for ** matcher because we were not yet able to resolve to a matching sitemap
        if(matchingSiteMapItem == null) {
            propertyPlaceHolderMap.clear();
            locationMapTreeItem = locationMapTree.getTreeItem(HstNodeTypes.ANY);
            if(locationMapTreeItem != null) {
                propertyPlaceHolderMap.put(KEY_TO_PROPERTY_PREFIX+String.valueOf(propertyPlaceHolderMap.size()+1), path);
                matchedLocationMapTreeItem =  locationMapTreeItem;
                matchingSiteMapItem = resolveToSiteMapItem(matchedLocationMapTreeItem);
            }
        }

        if(matchingSiteMapItem == null) {
            log.debug("Unable to linkrewrite '{}' to any sitemap item", path);
            return null;
        }
        
        Properties params = new Properties();
        for(Entry<String,String> entry : propertyPlaceHolderMap.entrySet()){
            Map<String,String> keyToPropertyPlaceHolderMap = ((HstSiteMapItemService)matchingSiteMapItem).getKeyToPropertyPlaceHolderMap();
            if(keyToPropertyPlaceHolderMap.containsKey(entry.getKey())) {
                // translate key1 -> n1, key2 -> n2 etc
                params.put(keyToPropertyPlaceHolderMap.get(entry.getKey()), entry.getValue());
            } else {
                if(!keyToPropertyPlaceHolderMap.containsValue(entry.getKey())) {
                    // inherited params from current ctx: when the keyToPropertyPlaceHolderMap contains the entry.getKey() as value, 
                    // the param is already mapped. 
                    params.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        PropertyParser pp = new PropertyParser(params);
        
        String resolvedPath = (String)pp.resolveProperty("parameterizedPath", ((HstSiteMapItemService)matchingSiteMapItem).getParameterizedPath());
        if(resolvedPath == null) {
            if(!isSubResolver) {
                log.debug("Unable to resolve '{}'. Return null", ((HstSiteMapItemService)matchingSiteMapItem).getParameterizedPath());
            }
            return null;
        }
        
        log.info("Succesfully rewrote path '{}' into new sitemap path '{}'", path, resolvedPath);
        log.debug("creating link for '{}' took '{}' ms.", path, String.valueOf((System.nanoTime() - start)/ 1000000D) );
        
        ResolvedLocationMapTreeItem r = new ResolvedLocationMapTreeItemImpl(resolvedPath, matchingSiteMapItem);


        return r;
      
    }

    /**
     * Extracts the correct sitemap item from the matchedLocationMapTreeItem. If there are multiple sitemap items in the 
     * matchedLocationMapTreeItem, we find the one that in closest to the current context, or, if <code>canonical</code> is true,
     * we return one without context. <code>null</code> is returned when no correct hstsitemap item can be extracted 
     * @param matchedLocationMapTreeItem
     * @return
     */
    private HstSiteMapItem resolveToSiteMapItem(LocationMapTreeItem matchedLocationMapTreeItem) {
        
        if(matchedLocationMapTreeItem == null || matchedLocationMapTreeItem.getHstSiteMapItems().size() == 0) {
            return null;
        }
        
        HstSiteMapItem hstSiteMapItem = null;

        List<HstSiteMapItem> typeMatchedSiteMapItems = new ArrayList<HstSiteMapItem>();
        List<HstSiteMapItem> fallbackSiteMapItems = new ArrayList<HstSiteMapItem>();
        for(HstSiteMapItem item : matchedLocationMapTreeItem.getHstSiteMapItems()) {
            HstSiteMapItemService serv = (HstSiteMapItemService)item;
            if(representsDocument) {
                if(serv.getExtension() != null) {
                    //  found a sitemap item with an extension! Add this one
                    typeMatchedSiteMapItems.add(serv);
                } else {
                    fallbackSiteMapItems.add(serv);
                }
            } else {
                if(serv.getExtension() == null) {
                    //  found a sitemap item without an extension! Add this one
                    typeMatchedSiteMapItems.add(serv);
                } else {
                    fallbackSiteMapItems.add(serv);
                }
            }
        }
        
        if(this.canonical || resolvedSiteMapItem == null) {
            // let's return the canonical location. 
            hstSiteMapItem = getCanonicalItem(typeMatchedSiteMapItems);
            if(hstSiteMapItem == null) {
                hstSiteMapItem = getCanonicalItem(fallbackSiteMapItems);
            }
        } else {
            // fetch the best matching sitemap item: 
            List<HstSiteMapItem> contextOrderedMatches = orderToBestInContext(typeMatchedSiteMapItems);
            for(HstSiteMapItem item : contextOrderedMatches) {
                hstSiteMapItem = contextualize((HstSiteMapItemService)item);  
                if(hstSiteMapItem != null) {
                    break;
                } 
            }
              
            if(hstSiteMapItem == null) {
                // check the fallback hst sitemap items
                List<HstSiteMapItem> contextFallBackOrderedMatches = orderToBestInContext(fallbackSiteMapItems);
                for(HstSiteMapItem item : contextFallBackOrderedMatches) {
                    
                    hstSiteMapItem = contextualize((HstSiteMapItemService)item);  
                    if(hstSiteMapItem != null) {
                        break;
                    } 
                }
            }
        }
          
        return hstSiteMapItem;
        
    }

    /*
     * The algorithm to find the canonical item is currently as follows:
     * 
     * 1) A sitemap item that returns true for isUseableInRightContextOnly() is not useable and ignored
     * 2) The sitemap item that has the lowest depth (getDepth()) is used as canonical.
     * 3) When multiple sitemap items have equal depth, the first is returned 
     * 
     * @param preferredSiteMapItems
     * @return the canonical <code>HstSiteMapItem</code> or <code>null</code> if not succeeded.
     */
    private HstSiteMapItem getCanonicalItem(List<HstSiteMapItem> matchedHstSiteMapItema) {
        List<HstSiteMapItem> canonicals = new ArrayList<HstSiteMapItem>();
        int lowestDepth = Integer.MAX_VALUE;
        for(HstSiteMapItem item : matchedHstSiteMapItema) {
            HstSiteMapItemService serviceItem = (HstSiteMapItemService)item;
            if(serviceItem.isUseableInRightContextOnly()) {
                // unuseable for canonical
                continue;
            }
            if(serviceItem.getDepth() < lowestDepth) {
                lowestDepth = serviceItem.getDepth();
                canonicals.clear();
                canonicals.add(serviceItem);
            } else if (serviceItem.getDepth() == lowestDepth) {
                canonicals.add(serviceItem);
            }
        }
        if(!canonicals.isEmpty()) {
            // even if multiple canonicals are equally suited, we cannot do better then pick the first
            return canonicals.get(0);
        }
        return null;
    }

    /*
     * Try if the sitemap item can only be used within a context, to attach the correct context params to it.
     * 
     * if isUseableInRightContextOnly but the context is not ok, null is returned
     */
    private HstSiteMapItem contextualize(HstSiteMapItemService matchedHstSiteMapItem) {
        
        if(matchedHstSiteMapItem.isUseableInRightContextOnly()) {
            boolean mergeable = mergeMatchedItemWithCurrentCtx(matchedHstSiteMapItem);
            
            // if not succeeded, return null
            if(!mergeable)  {
                log.debug("Cannot contextualize hstSiteMapItem '{}' for current sitemap item '{}'", matchedHstSiteMapItem.getId(), this.resolvedSiteMapItem.getHstSiteMapItem().getId());
                return null;
            }
            return matchedHstSiteMapItem;
        } else {
            return matchedHstSiteMapItem;
        }
        
    }

    /*
     * When the matched sitemap item can be resolved with the currentCtxResolvedSiteMapItem, true is returned
     * and the missing properties in the propertyPlaceHolderMap are added from the currentCtxResolvedSiteMapItem.
     */
    private boolean mergeMatchedItemWithCurrentCtx(HstSiteMapItemService matchedHstSiteMapItem) {
        
         
        // check whether *all* unresolved wildcards in the matchedHstSiteMapItem can be filled in by the currentCtxResolvedSiteMapItem
        
        LinkedList<HstSiteMapItemService> matchedAncestorOrSelfWildcardList = new LinkedList<HstSiteMapItemService>();
        HstSiteMapItemService matchedAncestorOrSelf = (HstSiteMapItemService)matchedHstSiteMapItem;
        while(matchedAncestorOrSelf != null) {
            if(matchedAncestorOrSelf.isWildCard() || matchedAncestorOrSelf.containsWildCard() || matchedAncestorOrSelf.isAny() || matchedAncestorOrSelf.containsAny()) {
                matchedAncestorOrSelfWildcardList.add(0,matchedAncestorOrSelf);
            }
            matchedAncestorOrSelf = (HstSiteMapItemService)matchedAncestorOrSelf.getParentItem();
        }
        
        LinkedList<HstSiteMapItemService> currentCtxAncestorOrSeldWildcardList = new LinkedList<HstSiteMapItemService>();
        HstSiteMapItemService currentAncestorOrSelf = (HstSiteMapItemService)this.resolvedSiteMapItem.getHstSiteMapItem();
        while(currentAncestorOrSelf != null) {
            if(currentAncestorOrSelf.isWildCard() || currentAncestorOrSelf.containsWildCard() || currentAncestorOrSelf.isAny() || currentAncestorOrSelf.containsAny()) {
                currentCtxAncestorOrSeldWildcardList.add(0,currentAncestorOrSelf);
            }
            currentAncestorOrSelf = (HstSiteMapItemService)currentAncestorOrSelf.getParentItem();
        }
        
        /*
         * Now, to the propertyPlaceHolderMap add all resolved shared wildcards between ancestors from matchedHstSiteMapItem and
         * currentCtxResolvedSiteMapItem
         */ 
        
        // iterate the matchedAncestorWildcardList and see which items are shared: the parameters in the linkedList can be inherited
        
        Properties currentCtxProperties = this.resolvedSiteMapItem.getParameters();
        Map<String, String> propertiesToMerge = new HashMap<String,String>();
        for(HstSiteMapItemService matchedAncestorItem : matchedAncestorOrSelfWildcardList) {
            int index = currentCtxAncestorOrSeldWildcardList.indexOf(matchedAncestorItem);
            if( index > -1 && currentCtxProperties.containsKey(String.valueOf(index+1))) {
                // add currentCtxAncestor parameter
                propertiesToMerge.put(String.valueOf(index+1), currentCtxProperties.getProperty(String.valueOf(index+1)));
            }
        }
        
        if(matchedHstSiteMapItem.getWildCardAnyOccurences() <= (propertiesToMerge.size() + propertyPlaceHolderMap.size())) {
            log.debug("Succeful contextualized matched sitemap item");
            propertyPlaceHolderMap.putAll(propertiesToMerge);
            return true;
        }
        log.debug("Unsucceful contextualized matched sitemap item");
        return false;
    }

    private LocationMapTreeItem resolveMatchingLocationMapTreeItem(LocationMapTreeItem locationMapTreeItem, int position, String[] elements) {
        return traverseInToLocationMapTreeItem(locationMapTreeItem, position, elements);
     }

     private LocationMapTreeItem traverseInToLocationMapTreeItem(LocationMapTreeItem locationMapTreeItem, int position, String[] elements) {
         
         checkedLocationMapTreeItems.add(locationMapTreeItem);
         if(position == elements.length) {
            // we are ready if this locationMapTreeItem contains at least one HstSiteMapItem
            if(locationMapTreeItem.getHstSiteMapItems().size() > 0) {
               return locationMapTreeItem;
            } 
            // there was a matched locationMapTreeItem, but it did not have sitemap items attached to it. Continue searching after 
            // a move up: if the current locationMapTreeItem is a wildcard, we need to remove the param wrt to wildcard again
            if(((LocationMapTreeItemImpl)locationMapTreeItem).isWildCard()) {
                propertyPlaceHolderMap.remove(KEY_TO_PROPERTY_PREFIX+(propertyPlaceHolderMap.size()));
            }
            return traverseUp(locationMapTreeItem.getParentItem(), position, elements);
        }
        if(locationMapTreeItem.getChild(elements[position]) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(elements[position]))) {
            return traverseInToLocationMapTreeItem(locationMapTreeItem.getChild(elements[position]), ++position, elements);
        } else if(locationMapTreeItem.getChild(HstNodeTypes.WILDCARD) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(HstNodeTypes.WILDCARD))) {
            propertyPlaceHolderMap.put(KEY_TO_PROPERTY_PREFIX+(propertyPlaceHolderMap.size()+1), elements[position]);
            return traverseInToLocationMapTreeItem(locationMapTreeItem.getChild(HstNodeTypes.WILDCARD), ++position, elements);
        } else if(locationMapTreeItem.getChild(HstNodeTypes.ANY) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(HstNodeTypes.ANY))) {
            checkedLocationMapTreeItems.add(locationMapTreeItem.getChild(HstNodeTypes.ANY));
            return getANYMatchingLocationMapTreeItem(locationMapTreeItem,position, elements);
        }  
        else {
            // We did not find a match for traversing this sitemap item tree. Traverse up, and try another tree
            return traverseUp(locationMapTreeItem, position, elements);
        }
        
     }


     private LocationMapTreeItem traverseUp(LocationMapTreeItem locationMapTreeItem, int position, String[] elements) {
        if(locationMapTreeItem == null) {
            return null;
        }
        if(((LocationMapTreeItemImpl)locationMapTreeItem).isWildCard()) {
            if(locationMapTreeItem.getChild(HstNodeTypes.WILDCARD) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(HstNodeTypes.WILDCARD))){
                return traverseInToLocationMapTreeItem(locationMapTreeItem, position, elements);
            } else if(locationMapTreeItem.getChild(HstNodeTypes.ANY) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(HstNodeTypes.ANY))){
                return traverseInToLocationMapTreeItem(locationMapTreeItem,position, elements);
            }
            // as this tree path did not result in a match, remove some params again
            propertyPlaceHolderMap.remove(KEY_TO_PROPERTY_PREFIX+(propertyPlaceHolderMap.size()));
            return traverseUp(locationMapTreeItem.getParentItem(), --position, elements );
        } else if(locationMapTreeItem.getChild(HstNodeTypes.WILDCARD) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(HstNodeTypes.WILDCARD))){
            return traverseInToLocationMapTreeItem(locationMapTreeItem, position, elements);
        } else if(locationMapTreeItem.getChild(HstNodeTypes.ANY) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(HstNodeTypes.ANY))){
            return traverseInToLocationMapTreeItem(locationMapTreeItem,position, elements);
        } else {    
            return traverseUp(locationMapTreeItem.getParentItem(), --position, elements );
        }
        
     }
     
     private LocationMapTreeItem getANYMatchingLocationMapTreeItem(LocationMapTreeItem locationMapTreeItem, int position, String[] elements) {
             StringBuffer remainder = new StringBuffer(elements[position]);
             while(++position < elements.length) {
                 remainder.append("/").append(elements[position]);
             }
             propertyPlaceHolderMap.put(KEY_TO_PROPERTY_PREFIX+(propertyPlaceHolderMap.size()+1), remainder.toString());
             return locationMapTreeItem.getChild(HstNodeTypes.ANY);
     }
    
    /*
     * The algorithm which sitemap item is best in context is as follows: 
     * 
     * 1) one of the N sitemap items is the same as the current ctx sitemapitem, this one is taken (SAME) : break;
     * 2) if one of the N sitemap items is a *descendant* sitemap item of the current ctx sitemap item, that one is taken : break;
     * 3) Take the matched sitemap items (List) with the first common ancestor with the current ctx sitemap item : if List contains 1 item: return item: else continue;
     * 4) If (3) returns multiple matched items, return the ones that are the closest (wrt depth) to the common ancestor: if there is one, return item : else continue;
     * 5) If (4) returns 1 or more items, pick the first (we cannot distinguish better) 
     * 6) If still no best context hit found, we return the first matchingSiteMapItem in the matchingSiteMapItems list, as there they are all equally out of context
     * 
     * if the list is empty, return null
     */
    private List<HstSiteMapItem> orderToBestInContext(List<HstSiteMapItem> matchingSiteMapItems) {
        if(matchingSiteMapItems.size() == 0 || matchingSiteMapItems.size() == 1) {
            return matchingSiteMapItems;
        }
        
        int bestDepth = Integer.MAX_VALUE;
        HstSiteMapItem bestMatch = null;
        
        List<HstSiteMapItem> unsortedItems = new ArrayList<HstSiteMapItem>(matchingSiteMapItems);
        List<HstSiteMapItem> contextOrderedMatches = new ArrayList<HstSiteMapItem>();
        for(HstSiteMapItem siteMapItem : matchingSiteMapItems) {
            // step (1) algorithm
            if(siteMapItem == this.resolvedSiteMapItem.getHstSiteMapItem()) {
                // step (1)  of the algorithm succeeded
                contextOrderedMatches.add(siteMapItem);
                unsortedItems.remove(siteMapItem);
                if(unsortedItems.size() == 1) {
                    contextOrderedMatches.add(unsortedItems.get(0));
                    return contextOrderedMatches;
                }
            }
            
            // step (2) algorithm
            int depth = 0;
            HstSiteMapItem current = siteMapItem;
            while(current.getParentItem() != null) {
                current = current.getParentItem();
                depth++;
                if(current == this.resolvedSiteMapItem.getHstSiteMapItem()) {
                    if(depth < bestDepth) {
                        bestMatch = siteMapItem;
                        bestDepth = depth;
                    }
                    break;
                }
            }
        }
        
        if(bestMatch != null) {
            // step (2) of the algorithm succeeded
            if(!contextOrderedMatches.contains(bestMatch)) {
                contextOrderedMatches.add(bestMatch);
                unsortedItems.remove(bestMatch);
                if(unsortedItems.size() == 1) {
                    contextOrderedMatches.add(unsortedItems.get(0));
                    return contextOrderedMatches;
                }
            }
        }
        
        // step (3) algorithm
        /*
         * A map containing as keys the matchingSiteMapItems and as values the sorted list of parent sitemap items of the key
         * 
         */
        Map<HstSiteMapItem, List<HstSiteMapItem>> matchingMapWithParents  = new HashMap<HstSiteMapItem, List<HstSiteMapItem>>();

        // a sorted list of parents, where the highest parents are last in the list
        
        for(HstSiteMapItem siteMapItem : matchingSiteMapItems) {
            HstSiteMapItem current = siteMapItem;
            List<HstSiteMapItem> parentList = new ArrayList<HstSiteMapItem>();
            while(current.getParentItem() != null) {
                current = current.getParentItem();
                parentList.add(current);
            }
            matchingMapWithParents.put(siteMapItem, parentList);
        }
        
        // now, for the current context item, we iterate up, and see which matchingSiteMapItems are the first to have a common ancestor as the current context. In the
        // Integer we store how deep the matchingSiteMapItem is below the common ancestor, which is needed in step (4) if we get there
        TreeMap<Integer, List<HstSiteMapItem>> commonAncestorMap = new TreeMap<Integer, List<HstSiteMapItem>>();
        
        HstSiteMapItem checkForCommonAncestor = this.resolvedSiteMapItem.getHstSiteMapItem();
        while(checkForCommonAncestor.getParentItem() != null && commonAncestorMap.size() == 0) {
            checkForCommonAncestor = checkForCommonAncestor.getParentItem();
            for(Entry<HstSiteMapItem, List<HstSiteMapItem>> entry : matchingMapWithParents.entrySet()) {
                if(entry.getValue().contains(checkForCommonAncestor)) {
                    // we found a common ancestor! Add to commonAncestorMap
                    List<HstSiteMapItem> items = commonAncestorMap.get(entry.getValue().indexOf(checkForCommonAncestor));
                    if(items == null) {
                        items = new ArrayList<HstSiteMapItem>();
                        items.add(entry.getKey());
                        commonAncestorMap.put(entry.getValue().indexOf(checkForCommonAncestor), items);
                    } else {
                        items.add(entry.getKey());
                    }
               }
            }
        } 
        
       
        if (commonAncestorMap.size() > 0) {
            // step (4) and (5) we have multiple matching sitemap items with a common first ancestor: now, find the sitemap item that is closest 
            // to this ancestor. If there are multiple with equal depth to the ancestor, we return the first (see step (5))
            
            // to be sure, do not check deeper then 25 levels, this is not reasonable ;-)
            int i = 0;
            while(i <= 25) {
                if(commonAncestorMap.containsKey(i)) {
                    // we found one with minimum depth: return this one:
                    for(HstSiteMapItem item : commonAncestorMap.get(i)) {
                        if(!contextOrderedMatches.contains(item)) {
                            contextOrderedMatches.add(item);
                            unsortedItems.remove(item);
                            if(unsortedItems.size() == 1) {
                                contextOrderedMatches.add(unsortedItems.get(0));
                                return contextOrderedMatches;
                            }
                        }
                    }
                }
                i++;
            }
        }
        if(matchingSiteMapItems.size() > 0) {
            // we are at step (6) : add the ones we did not yet add:
            for(HstSiteMapItem item : matchingSiteMapItems) {
                if(!contextOrderedMatches.contains(item)) {
                    contextOrderedMatches.add(item);
                }
            }
        }
        
        return contextOrderedMatches;
    }

}
