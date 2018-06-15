/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.platform.configuration.sitemap.HstSiteMapItemService;
import org.hippoecm.hst.core.internal.CollectionOptimizer;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.hippoecm.hst.core.linking.LocationMapTreeItem;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocationMapTreeSiteMap implements LocationMapTree {

    private static final String KEY_TO_PROPERTY_PREFIX = "key";

    private final static Logger log = LoggerFactory.getLogger(LocationMapTreeSiteMap.class);
    
    private Map<String, LocationMapTreeItem> children = new HashMap<>();


    public LocationMapTreeSiteMap(final List<HstSiteMapItem> siteMapItems) {
        for(HstSiteMapItem siteMapItem : siteMapItems ){
            add2LocationMap(siteMapItem);
        }
        optimize();
    }


    public LocationMapTreeSiteMap(final HstSiteMapItem siteMapItem) {
        add2LocationMap(siteMapItem);
        optimize();
    }

    private void add2LocationMap(final HstSiteMapItem siteMapItem) {

        final String normPath = PathUtils.normalizePath(siteMapItem.getRelativeContentPath());

        if (siteMapItem.isExcludedForLinkRewriting()){
            log.debug("'{}' will not be used for link rewriting as it is marked deleted or is configured to be " +
                    "excluded for link rewriting.", siteMapItem);
            return;
        }

        if (StringUtils.isNotEmpty(normPath)) {
            log.debug("Adding to location map path '{}' for sitemap item '{}'", normPath, siteMapItem.getQualifiedId());
            addSiteMapItem(normPath, siteMapItem);
        }

        for(HstSiteMapItem child : siteMapItem.getChildren()) {
           add2LocationMap(child);
        }
    }


    private void addSiteMapItem(final String unresolvedPath, final HstSiteMapItem siteMapItem) {
        if(unresolvedPath == null) {
            log.debug("'{}' will not be used for linkrewriting as it has an empty relative content path.", siteMapItem);
            return;
        }

        final List<String> propertyOrderList = new ArrayList<>();
        
        final Properties params = new Properties();
        // see if there are any SiteMapItems which are wildcard or any matchers and set these as properties.
        
        // list of the sitemap items ancestors + the sitemap item itself. The last item in the list is the top parent
        final List<HstSiteMapItem> ancestorItems = new ArrayList<>();
        ancestorItems.add(siteMapItem);
        HstSiteMapItem parent = siteMapItem.getParentItem();
        while (parent != null) {
            ancestorItems.add(parent);
            parent = parent.getParentItem();
        }
        
        // traverse the ancestors list now to see if there are wildcard or any matchers
        if (!siteMapItem.isExplicitPath()) {
            int index = ancestorItems.size();
            while (index-- != 0) {
                final HstSiteMapItemService s = (HstSiteMapItemService) ancestorItems.get(index);
                if (s.isWildCard()) {
                    params.put(String.valueOf(params.size() + 1), HstNodeTypes.WILDCARD);
                    propertyOrderList.add(PropertyParser.DEFAULT_PLACEHOLDER_PREFIX + params.size() + PropertyParser.DEFAULT_PLACEHOLDER_SUFFIX);
                } else if (s.isAny()) {
                    params.put(String.valueOf(params.size() + 1), HstNodeTypes.ANY);
                    propertyOrderList.add(PropertyParser.DEFAULT_PLACEHOLDER_PREFIX + params.size() + PropertyParser.DEFAULT_PLACEHOLDER_SUFFIX);
                } else if (s.containsWildCard()) {
                    // we assume a postfix containing a "." only meant for document url extension, disregard for linkmatching first
                    String paramVal = s.getPrefix() + HstNodeTypes.WILDCARD;
                    if (s.getPostfix().indexOf(".") > -1) {
                        final String post = s.getPostfix().substring(0, s.getPostfix().indexOf("."));
                        if (!"".equals(post)) {
                            paramVal += post;
                        }
                    } else {
                        paramVal += s.getPostfix();
                    }
                    params.put(String.valueOf(params.size() + 1), paramVal);
                    propertyOrderList.add(PropertyParser.DEFAULT_PLACEHOLDER_PREFIX + params.size() + PropertyParser.DEFAULT_PLACEHOLDER_SUFFIX);
                } else if (s.containsAny()) {
                    // we assume a postfix containing a "." only meant for document url extension, disregard for linkmatching first
                    String paramVal = s.getPrefix() + HstNodeTypes.ANY;
                    if (s.getPostfix().indexOf(".") > -1) {
                        String post = s.getPostfix().substring(0, s.getPostfix().indexOf("."));
                        if (!"".equals(post)) {
                            paramVal += post;
                        }
                    } else {
                        paramVal += s.getPostfix();
                    }
                    params.put(String.valueOf(params.size() + 1), paramVal);
                    propertyOrderList.add(PropertyParser.DEFAULT_PLACEHOLDER_PREFIX + params.size() + PropertyParser.DEFAULT_PLACEHOLDER_SUFFIX);
                }
            }
        }

        final Map<String, String> keyToPropertyPlaceHolderMap = new HashMap<>();

        final String[] unresolvedPathEls = unresolvedPath.split("/");
        int keyNumber = 1;
        
        for(String pathEl : unresolvedPathEls) {
            final int loc = propertyOrderList.indexOf(pathEl);
            if(loc > -1) {
                keyToPropertyPlaceHolderMap.put(KEY_TO_PROPERTY_PREFIX+keyNumber, String.valueOf(loc + 1));
                keyNumber++;
            }
        }
        ((HstSiteMapItemService)siteMapItem).setKeyToPropertyPlaceHolderMap(keyToPropertyPlaceHolderMap);
        
        final PropertyParser pp = new PropertyParser(params);
        
        /*
         * If and only IF all property placeholders do occur in the 'unresolvedPath' it is possible to use this 
         * sitemapItem plus relative contentpath in the LocationMapTree without a context: think about it: If my sitemap path would be 
         * '_default_/home' (_default_ = * ) and the relative content path would be /common/home, than obviously, we cannot
         * create a link to this sitemap item if we do not have a context: namely, what would we take for _default_ ? If the relative content path would be 
         * ${1}/home, then we can use it. Therefor, we check below whether all property placeholders are resolved. If not, then this LocationMapTreeItem
         * can only be used with a context: if for example, the current url is /foo/home, which matches to '_default_/home', and the relative 
         * content path = /common/home, then we can conclude that the link must become: /foo/home  
         * 
         */
        
        for(Object param : params.keySet()) {
            final String propertyPlaceHolder = PropertyParser.DEFAULT_PLACEHOLDER_PREFIX + param + PropertyParser.DEFAULT_PLACEHOLDER_SUFFIX;
            if(!unresolvedPath.contains(propertyPlaceHolder)) {
                log.debug("The SiteMapItem with id '{}' and relative content path '{}' can only be used in a context aware link rewriting",
                        siteMapItem.getId(), siteMapItem.getRelativeContentPath());
                ((HstSiteMapItemService)siteMapItem).setUseableInRightContextOnly(true);
            }
        }
        
        final String resolvedPath = (String)pp.resolveProperty("relative contentpath", unresolvedPath);

        if (resolvedPath == null) {
            log.warn("Skipping '{}' for linkrewriting : Unable to translate relative content path '{}' " +
                            "because the wildcards in '{}' do not match the property placeholders in the " +
                            "relative content path. ",
                    siteMapItem, unresolvedPath, siteMapItem);
            return;
        } 
        log.debug("Translated relative contentpath '{}' --> '{}'", unresolvedPath, resolvedPath);

        addSiteMapItem(resolvedPath.split("/"), siteMapItem);
    }

    private void addSiteMapItem(final String[] pathFragments,
                                final HstSiteMapItem siteMapItem){
        if (pathFragments.length == 0) {
            return;
        }
        LocationMapTreeItemImpl child = (LocationMapTreeItemImpl) getTreeItem(pathFragments[0]);
        if(child == null) {
            child = new LocationMapTreeItemImpl();
            this.children.put(StringPool.get(pathFragments[0]), child);
        }
        child.addSiteMapItem(pathFragments, siteMapItem, 1);
    }

    public LocationMapTreeItem getTreeItem(final String name) {
        return children.get(name);
    }


    private void optimize() {
        children = CollectionOptimizer.optimizeHashMap(children);
        for (LocationMapTreeItem child : children.values()) {
            ((LocationMapTreeItemImpl)child).optimize();
        }
    }

}
