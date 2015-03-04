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
package org.hippoecm.hst.core.linking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItemService;
import org.hippoecm.hst.core.internal.CollectionOptimizer;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocationMapTreeImpl implements LocationMapTree {

    private static final String KEY_TO_PROPERTY_PREFIX = "key";

    private final static Logger log = LoggerFactory.getLogger(LocationMapTreeImpl.class);
    
    private Map<String, LocationMapTreeItem> children = new HashMap<>();

    public LocationMapTreeImpl(final List<HstSiteMapItem> siteMapItems) {
        this(siteMapItems, null, null);
    }

    public LocationMapTreeImpl(final List<HstSiteMapItem> siteMapItems,
                               final HstComponentsConfiguration configuration,
                               final String mountContentPath) {
        for(HstSiteMapItem siteMapItem : siteMapItems ){
            add2LocationMap(siteMapItem, configuration, mountContentPath);
        }
        optimize();
    }

    public LocationMapTreeImpl(final HstSiteMapItem siteMapItem) {
        this(siteMapItem, null, null);
    }

    public LocationMapTreeImpl(final HstSiteMapItem siteMapItem,
                               final HstComponentsConfiguration configuration,
                               final String mountContentPath) {
        add2LocationMap(siteMapItem, configuration, mountContentPath);
        optimize();
    }

    private void add2LocationMap(final HstSiteMapItem siteMapItem,
                                 final HstComponentsConfiguration configuration,
                                 final String mountContentPath) {

        String normPath = PathUtils.normalizePath(siteMapItem.getRelativeContentPath());
        if (StringUtils.isNotEmpty(normPath)) {
            log.debug("Adding to location map path '{}' for sitemap item '{}'", normPath, siteMapItem.getQualifiedId());
            addSiteMapItem(normPath, siteMapItem);
        }

        // for explicit sitemap item paths (no wildcards in it) include possible component item picked documents
        if (configuration != null && siteMapItem.getComponentConfigurationId() != null && siteMapItem.isExplicitPath()) {
            final HstComponentConfiguration cc = configuration.getComponentConfiguration(siteMapItem.getComponentConfigurationId());
            if (cc == null) {
                log.warn("Sitemap item '{}' for site '{}' contains unresolvable hst:componentconfigurationid '{}'.",
                        siteMapItem.getQualifiedId(), siteMapItem.getHstSiteMap().getSite().getName(), siteMapItem.getComponentConfigurationId());
            } else {

                // find all extra document paths possibly stored in the components belonging to the page of this siteMapItem
                List<String> documentPaths =  DocumentParamsScanner.findDocumentPathsRecursive(cc);

                Properties siteMapItemParameters = new Properties();
                for (Map.Entry<String, String> param : siteMapItem.getParameters().entrySet()) {
                    siteMapItemParameters.put(param.getKey(), param.getValue());
                }
                PropertyParser pp = new PropertyParser(siteMapItemParameters);
                for (String documentPath : documentPaths) {
                    // documentPath can have property place holders referring to a property from sitemap item, for example
                    // images/${bannerlocation} hence we need to resolve these first
                    String parsedDocumentPath = (String)pp.resolveProperty("documentPath", documentPath);

                    if (parsedDocumentPath.startsWith("/")) {
                        // absolute path, strip the current mount root path and if does not start with mount root path
                        // skip the document link.
                        if (mountContentPath == null) {
                            continue;
                        }
                        if (!parsedDocumentPath.startsWith(mountContentPath)) {
                            continue;
                        }
                        parsedDocumentPath = parsedDocumentPath.substring(mountContentPath.length());
                    }
                    String normalizedParsedDocumentPath = PathUtils.normalizePath(parsedDocumentPath);
                    if (StringUtils.isNotEmpty(normalizedParsedDocumentPath)) {
                        log.debug("Adding document path '{}' from page to location map for sitemap item '{}'",
                                normalizedParsedDocumentPath, siteMapItem.getQualifiedId());
                        // TODO mark this item that it is linked via component document to be later in a position to
                        // know the difference between sitemap item relative content path linked and component linked
                        // items
                        addSiteMapItem(normalizedParsedDocumentPath, siteMapItem);
                    }
                }
            }
        }

        for(HstSiteMapItem child : siteMapItem.getChildren()) {
           add2LocationMap(child, configuration, mountContentPath);
        }
    }


    private void addSiteMapItem(String unresolvedPath, HstSiteMapItem hstSiteMapItem) {
        if(unresolvedPath == null) {
            log.debug("HstSiteMapItem '{}' will not be used for linkrewriting as it has an empty relative content path.", hstSiteMapItem.getId());
            return;
        }
        if(hstSiteMapItem.isExcludedForLinkRewriting()){
            log.debug("HstSiteMapItem '{}' will not be used for linkrewriting as is configured to be excluded for linkrewriting.", hstSiteMapItem.getId());
            return;
        }
        
        List<String> propertyOrderList = new ArrayList<String>();
        
        Properties params = new Properties();
        // see if there are any SiteMapItems which are wildcard or any matchers and set these as properties.
        
        // list of the sitemap items ancestors + the sitemap item itself. The last item in the list is the top parent
        List<HstSiteMapItem> ancestorItems = new ArrayList<HstSiteMapItem>();
        ancestorItems.add(hstSiteMapItem);
        HstSiteMapItem parent = hstSiteMapItem.getParentItem();
        while (parent != null) {
            ancestorItems.add(parent);
            parent = parent.getParentItem();
        }
        
        // traverse the ancestors list now to see if there are wildcard or any matchers
        if (!hstSiteMapItem.isExplicitPath()) {
            int index = ancestorItems.size();
            while (index-- != 0) {
                HstSiteMapItemService s = (HstSiteMapItemService) ancestorItems.get(index);
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
                        String post = s.getPostfix().substring(0, s.getPostfix().indexOf("."));
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
        
        Map<String, String> keyToPropertyPlaceHolderMap = new HashMap<String,String>();
        
        String[] unresolvedPathEls = unresolvedPath.split("/");
        int keyNumber = 1;
        
        for(String pathEl : unresolvedPathEls) {
            int loc = propertyOrderList.indexOf(pathEl);
            if(loc > -1) {
                keyToPropertyPlaceHolderMap.put(KEY_TO_PROPERTY_PREFIX+keyNumber, String.valueOf(loc + 1));
                keyNumber++;
            }
        }
        ((HstSiteMapItemService)hstSiteMapItem).setKeyToPropertyPlaceHolderMap(keyToPropertyPlaceHolderMap);
        
        PropertyParser pp = new PropertyParser(params);
        
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
            String propertyPlaceHolder = PropertyParser.DEFAULT_PLACEHOLDER_PREFIX + ((String)param) + PropertyParser.DEFAULT_PLACEHOLDER_SUFFIX;
            if(!unresolvedPath.contains(propertyPlaceHolder)) {
                log.debug("The SiteMapItem with id '{}' and relative content path '{}' can only be used in a context aware link rewriting", hstSiteMapItem.getId(), hstSiteMapItem.getRelativeContentPath());
                ((HstSiteMapItemService)hstSiteMapItem).setUseableInRightContextOnly(true);
            }
        }
        
        String resolvedPath = (String)pp.resolveProperty("relative contentpath", unresolvedPath);
       
        if(resolvedPath == null) {
            log.warn("Skipping sitemap item '{}' for linkrewriting : Unable to translate relative content path '{}' " +
                    "because the wildcards in sitemap item path ('{}') do not match the property placeholders in the " +
                    "relative content path. ",
                    hstSiteMapItem.getQualifiedId(), unresolvedPath, hstSiteMapItem.getId());
            return;
        } 
        log.debug("Translated relative contentpath '{}' --> '{}'", unresolvedPath, resolvedPath);
        
        List<String> pathFragment = new ArrayList<String>(Arrays.asList(resolvedPath.split("/")));
        
        addSiteMapItem(pathFragment, hstSiteMapItem);
    }
    
    private void addSiteMapItem(List<String> pathFragment, HstSiteMapItem hstSiteMapItem){
        
        LocationMapTreeItemImpl child = (LocationMapTreeItemImpl) getTreeItem(pathFragment.get(0));
        if(child == null) {
            child = new LocationMapTreeItemImpl();
            this.children.put(StringPool.get(pathFragment.get(0)), child);
        }
        pathFragment.remove(0);
        child.addSiteMapItem(pathFragment , hstSiteMapItem);
    }


    public LocationMapTreeItem getTreeItem(String name) {
        return children.get(name);
    }


    private void optimize() {
        children = CollectionOptimizer.optimizeHashMap(children);
        for (LocationMapTreeItem child : children.values()) {
            ((LocationMapTreeItemImpl)child).optimize();
        }
    }

}
