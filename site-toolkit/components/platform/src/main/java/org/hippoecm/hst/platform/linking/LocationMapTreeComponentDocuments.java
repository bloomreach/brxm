/*
 *  Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.CollectionOptimizer;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.core.linking.DocumentParamsScanner;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.hippoecm.hst.core.linking.LocationMapTreeItem;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.cms7.services.ServletContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocationMapTreeComponentDocuments implements LocationMapTree {

    private final static Logger log = LoggerFactory.getLogger(LocationMapTreeComponentDocuments.class);

    private Map<String, LocationMapTreeItem> children = new HashMap<>();

    public LocationMapTreeComponentDocuments(final List<HstSiteMapItem> siteMapItems,
                                             final HstComponentsConfiguration configuration,
                                             final String mountContentPath,
                                             final String contextPath) {
        if (configuration == null) {
            return;
        }
        for (HstSiteMapItem siteMapItem : siteMapItems) {
            add2LocationMap(siteMapItem, configuration, mountContentPath, getClassLoader(contextPath));
        }
        optimize();
    }

    public LocationMapTreeComponentDocuments(final HstSiteMapItem siteMapItem,
                                             final HstComponentsConfiguration configuration,
                                             final String mountContentPath,
                                             final String contextPath) {
        if (configuration == null) {
            return;
        }
        add2LocationMap(siteMapItem, configuration, mountContentPath, getClassLoader(contextPath));
        optimize();
    }


    private void add2LocationMap(final HstSiteMapItem siteMapItem,
                                 final HstComponentsConfiguration configuration,
                                 final String mountContentPath,
                                 final ClassLoader classLoader) {

        if (classLoader == null) {
            log.info("ClassLoader null. return without populating location map.");
            return;
        }

        if (siteMapItem.isExcludedForLinkRewriting()) {
            log.debug("'{}' will not be used for link rewriting as it is marked deleted or is configured to be " +
                    "excluded for link rewriting.", siteMapItem);
            return;
        }

        // for explicit sitemap item paths (no wildcards in it) include possible component item picked documents
        if (siteMapItem.getComponentConfigurationId() != null && siteMapItem.isExplicitPath()) {
            final HstComponentConfiguration cc = configuration.getComponentConfiguration(siteMapItem.getComponentConfigurationId());
            if (cc == null) {
                log.warn("'{}' for site '{}' contains unresolvable hst:componentconfigurationid '{}'.",
                        siteMapItem, siteMapItem.getHstSiteMap().getSite().getName(), siteMapItem.getComponentConfigurationId());
            } else {

                // find all extra document paths possibly stored in the components belonging to the page of this siteMapItem
                final List<String> documentPaths = DocumentParamsScanner.findDocumentPathsRecursive(cc, classLoader);

                final Properties siteMapItemParameters = new Properties();
                for (Map.Entry<String, String> param : siteMapItem.getParameters().entrySet()) {
                    if (param.getKey() == null || param.getValue() == null) {
                        // Properties (Hashtable) throws NPE on null key or value
                        continue;
                    }
                    siteMapItemParameters.put(param.getKey(), param.getValue());
                }
                final PropertyParser pp = new PropertyParser(siteMapItemParameters);
                for (String documentPath : documentPaths) {
                    // documentPath can have property place holders referring to a property from sitemap item, for example
                    // images/${bannerlocation} hence we need to resolve these first
                    String parsedDocumentPath = (String) pp.resolveProperty("documentPath", documentPath);
                    if (parsedDocumentPath == null) {
                        log.debug("Could not parse '{}' for '{}'. ", documentPath, siteMapItem);
                        continue;
                    }
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
                    final String normalizedParsedDocumentPath = PathUtils.normalizePath(parsedDocumentPath);
                    if (StringUtils.isNotEmpty(normalizedParsedDocumentPath)) {
                        log.debug("Adding document path '{}' from page to location map for sitemap item '{}'",
                                normalizedParsedDocumentPath, siteMapItem.getQualifiedId());
                        addSiteMapItem(normalizedParsedDocumentPath.split("/"), siteMapItem);
                    }
                }
            }
        }

        for (HstSiteMapItem child : siteMapItem.getChildren()) {
            add2LocationMap(child, configuration, mountContentPath, classLoader);
        }
    }

    private void addSiteMapItem(final String[] pathFragments,
                                final HstSiteMapItem siteMapItem) {
        if (pathFragments.length == 0) {
            return;
        }
        LocationMapTreeItemImpl child = (LocationMapTreeItemImpl) getTreeItem(pathFragments[0]);
        if (child == null) {
            child = new LocationMapTreeItemImpl();
            this.children.put(StringPool.get(pathFragments[0]), child);
        }
        child.addSiteMapItem(pathFragments, siteMapItem, 1);
    }

    public LocationMapTreeItem getTreeItem(final String name) {
        return children.get(name);
    }


    /**
     * @return the {@link ClassLoader} for the webapp belonging to <code>contextPath</code> and <code>null</code> if none found.
     * If no class loader is found, the LocationMapTreeComponentDocuments cannot be populated and returns with empty
     * Map<String, LocationMapTreeItem> children
     */
    private ClassLoader getClassLoader(final String contextPath) {
        final ServletContext context = ServletContextRegistry.getContext(contextPath);
        if (context == null) {
            log.warn("Cannot populate LocationMapTreeComponentDocuments because cannot find a ClassLoader for contextPath '{}'",
                    contextPath);
            return null;
        }
        return context.getClassLoader();
    }


    private void optimize() {
        children = CollectionOptimizer.optimizeHashMap(children);
        for (LocationMapTreeItem child : children.values()) {
            ((LocationMapTreeItemImpl) child).optimize();
        }
    }

}
