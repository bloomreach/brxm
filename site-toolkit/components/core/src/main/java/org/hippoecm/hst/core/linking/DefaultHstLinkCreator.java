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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.provider.jcr.JCRUtilities;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHstLinkCreator implements HstLinkCreator {

    private static final Logger log = LoggerFactory.getLogger(DefaultHstLinkCreator.class);

    private final static String DEFAULT_PAGE_NOT_FOUND_PATH = "pagenotfound";
    private String[] binaryLocations;
    private String binariesPrefix;
    private String pageNotFoundPath = DEFAULT_PAGE_NOT_FOUND_PATH;
    private Map<HstSiteMapItem, LocationMapTree> loadedSubLocationMapTree = Collections.synchronizedMap(new HashMap<HstSiteMapItem, LocationMapTree>());
    private HstLinkProcessor linkProcessor;
    
    private List<LocationResolver> locationResolvers;
    
    public void setBinariesPrefix(String binariesPrefix){
        this.binariesPrefix = PathUtils.normalizePath(binariesPrefix);
    }
    
    public void setBinaryLocations(String[] binaryLocations) {
        this.binaryLocations = binaryLocations;
    }
    
    public void setlinkProcessor(HstLinkProcessor linkProcessor) {
        this.linkProcessor = linkProcessor;
    }
    
    public void setLocationResolvers(List<LocationResolver> locationResolvers){
        this.locationResolvers = locationResolvers;
    }
    
    public List<LocationResolver> getLocationResolvers(){
        return this.locationResolvers;
    }
    
    public void setPageNotFoundPath(String pageNotFoundPath){
        this.pageNotFoundPath = PathUtils.normalizePath(pageNotFoundPath);
    }
    
    
    
    /**
     * If the uuid points to a node that is of type hippo:document and it is below a hippo:handle, we will
     * rewrite the link wrt hippo:handle, because a handle is the umbrella of a document.
     * 
     * If the uuid cannot be found, we return null
     * 
     * {@inheritDoc}
     */
    public HstLink create(String uuid, Session session, HstRequestContext requestContext) {
        try {
            Node node = session.getNodeByUUID(uuid);
            return create(node, requestContext);
        } catch (ItemNotFoundException e) {
            log.warn("Node with uuid '{}' cannot be found. Cannot create a HstLink, return null", uuid);
        } catch (RepositoryException e) {
            log.warn("RepositoryException Cannot create a HstLink, return null", uuid);
        } 
        return null;
    }

    public HstLink create(HippoBean bean, HstRequestContext hstRequestContext) {
        return create(bean.getNode(), hstRequestContext);
    }
    
    
    public HstLink create(Node node, HstRequestContext hstRequestContext) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, hstRequestContext);
        return linkResolver.resolve();
    }
    
    
    public HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem,
            boolean fallback) {
        return this.create(node, requestContext, preferredItem, fallback, false);
    }
    
    public HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem,
            boolean fallback, boolean navigationStateful) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, requestContext);
        linkResolver.preferredItem = preferredItem;
        linkResolver.fallback = fallback;
        linkResolver.navigationStateful = navigationStateful;
        return linkResolver.resolve();
    }
    
    public HstLink createCanonical(Node node, HstRequestContext requestContext) {
        return this.createCanonical(node, requestContext, null);
    }

    public HstLink createCanonical(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, requestContext);
        linkResolver.canonicalLink = true;
        linkResolver.preferredItem = preferredItem;
        // when no canonical can be found for the preferred item, we fallback to linkrewriting without the canonical 
        linkResolver.fallback = true;
        return linkResolver.resolve();
    }
    
    public HstLink create(Node node, HstSite hstSite) {
        return create(node, hstSite.getSiteMount());
    }
    
    public HstLink create(Node node, SiteMount siteMount) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, siteMount);
        return linkResolver.resolve();
    }
    
    public HstLink create(Node node, HstRequestContext requestContext,  String siteMountAlias) {
        SiteMount currentSiteMount = requestContext.getResolvedSiteMount().getSiteMount();
        SiteMount targetSiteMount = null; 
        for(String type: currentSiteMount.getTypes()) {
            targetSiteMount = requestContext.getVirtualHost().getVirtualHosts().getSiteMountByGroupAliasAndType(currentSiteMount.getVirtualHost().getHostGroupName(), siteMountAlias, type);
            if(targetSiteMount != null) {
                break;
            }
        }
        if(targetSiteMount == null) {
            StringBuffer types = new StringBuffer();
            for(String type: currentSiteMount.getTypes()) {
                if(types.length() > 0) {
                    types.append(",");
                }
                types.append(type);
            }
            String[] messages = {siteMountAlias , currentSiteMount.getVirtualHost().getHostGroupName(), types.toString()};
            log.warn("Cannot create a link for siteMountAlias '{}' as it cannot be found in the host group '{}' and one of the the types '{}'", messages);
            return null;
        }
        
        log.debug("Target SiteMount found for siteMountAlias '{}'. Create link for target site mount", siteMountAlias);
        return create(node, targetSiteMount);
    }


    public HstLink create(Node node, HstRequestContext requestContext,  String siteMountAlias, String type) {
        SiteMount targetSiteMount = requestContext.getVirtualHost().getVirtualHosts().getSiteMountByGroupAliasAndType(requestContext.getVirtualHost().getHostGroupName(), siteMountAlias, type);
        if(targetSiteMount == null) {
            String[] messages = {siteMountAlias , requestContext.getVirtualHost().getHostGroupName(), type};
            log.warn("Cannot create a link for siteMountAlias '{}' as it cannot be found in the host group '{}' for type '{}'", messages);
            return null;
        }
        log.debug("Target SiteMount found for siteMountAlias '{}'. Create link for target site mount", siteMountAlias);
        return create(node, targetSiteMount);
    }
    public HstLink create(String path, SiteMount siteMount) {
        return postProcess(new HstLinkImpl(PathUtils.normalizePath(path), siteMount));
    }
    
    public HstLink create(String path, SiteMount siteMount, boolean containerResource) {
        return postProcess(new HstLinkImpl(PathUtils.normalizePath(path), siteMount, containerResource));
    }

    /**
     * If the uuid points to a node that is of type hippo:document and it is below a hippo:handle, we will
     * rewrite the link wrt hippo:handle, because a handle is the umbrella of a document.
     * 
     * If the uuid cannot be found, we return null
     * 
     * {@inheritDoc}
     */
    @Deprecated
    public HstLink create(String uuid, Session session, ResolvedSiteMapItem resolvedSiteMapItem) {
        try {
            Node node = session.getNodeByUUID(uuid);
            return create(node, resolvedSiteMapItem);
        } catch (ItemNotFoundException e) {
            log.warn("Node with uuid '{}' cannot be found. Cannot create a HstLink, return null", uuid);
        } catch (RepositoryException e) {
            log.warn("RepositoryException Cannot create a HstLink, return null", uuid);
        } 
        return null;
    }
    
    @Deprecated
    public HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, resolvedSiteMapItem);
        return linkResolver.resolve();
    }

    @Deprecated
    public HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem,
            boolean fallback) {
        return this.create(node, resolvedSiteMapItem, preferredItem, fallback, false);
    }
    
    @Deprecated
    public HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem,
            boolean fallback, boolean navigationStateful) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, resolvedSiteMapItem);
        linkResolver.preferredItem = preferredItem;
        linkResolver.fallback = fallback;
        linkResolver.navigationStateful = navigationStateful;
        return linkResolver.resolve();
    }

    @Deprecated
    public HstLink create(String path, HstSite hstSite) {
        return create(path, hstSite.getSiteMount());
    }

    @Deprecated
    public HstLink create(String path, HstSite hstSite, boolean containerResource) {
        return postProcess(new HstLinkImpl(PathUtils.normalizePath(path), hstSite.getSiteMount(), containerResource));
    }
    
    @Deprecated
    public HstLink create(HstSiteMapItem toHstSiteMapItem) {
        return postProcess(new HstLinkImpl(getPath(toHstSiteMapItem), toHstSiteMapItem.getHstSiteMap().getSite().getSiteMount()));
    }

    @Deprecated
    public HstLink create(HstSite hstSite, String toSiteMapItemId) {
        HstSiteMapItem siteMapItem = hstSite.getSiteMap().getSiteMapItemById(toSiteMapItemId);

        if (siteMapItem == null) {
            if (log.isWarnEnabled()) {
                log.warn("No sitemap item found for id '{}' within Site '{}'. Cannot create link.", toSiteMapItemId,
                        hstSite.getName());
            }
            return null;
        }
        
        return postProcess(new HstLinkImpl(getPath(siteMapItem), hstSite.getSiteMount()));
    }

    @Deprecated
    public HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem) {
        return this.createCanonical(node, resolvedSiteMapItem, null);
    }

    @Deprecated
    public HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, resolvedSiteMapItem);
        linkResolver.canonicalLink = true;
        linkResolver.preferredItem = preferredItem;
        // when no canonical can be found for the preferred item, we fallback to linkrewriting without the canonical 
        linkResolver.fallback = true;
        return linkResolver.resolve();
    } 
    
    private HstLink postProcess(HstLink link) {
        if(linkProcessor != null) {
            link = linkProcessor.postProcess(link);
        }
        return link;
    }
    
    /**
     * @param siteMapItem
     * @return String representation of the path
     */
    public static String getPath(HstSiteMapItem siteMapItem) {
        StringBuffer path = new StringBuffer(siteMapItem.getValue());
        while (siteMapItem.getParentItem() != null) {
            siteMapItem = siteMapItem.getParentItem();
            path.insert(0, "/").insert(0, siteMapItem.getValue());
        }
        return path.toString();
    }
    
    public static String getPath(HstSiteMapItem siteMapItem, String relPath) {
        StringBuffer path = new StringBuffer(siteMapItem.getValue());
        while (siteMapItem.getParentItem() != null) {
            siteMapItem = siteMapItem.getParentItem();
            path.insert(0, "/").insert(0, siteMapItem.getValue());
        }
        if(relPath == null) {
            return path.toString();
        }
        if(relPath.startsWith("/")) {
            path.append(relPath);
        } else {
            path.append("/").append(relPath);
        }
        return path.toString();
    }

    public String getBinariesPrefix() {
        return this.binariesPrefix == null ? "" : this.binariesPrefix;
    }

    public boolean isBinaryLocation(String path) {
        if(binaryLocations == null || path == null) {
            return false;
        }
        for(String prefix : this.binaryLocations) {
            if(path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    
    private LocationMapResolver getSubLocationMapResolver(HstSiteMapItem preferredItem) {
        LocationMapTree subLocationMapTree = loadedSubLocationMapTree.get(preferredItem);;
        if(subLocationMapTree == null) {
            List<HstSiteMapItem> subRootItems = new ArrayList<HstSiteMapItem>();
            subRootItems.add(preferredItem);
            subLocationMapTree = new LocationMapTreeImpl(subRootItems);
            loadedSubLocationMapTree.put(preferredItem, subLocationMapTree);
        }
        return new LocationMapResolver(subLocationMapTree);
    }

    
    private class HstLinkResolver {
        
        Node node;
        String nodePath;
       
        ResolvedSiteMapItem resolvedSiteMapItem;
        SiteMount siteMount;
        
        HstSiteMapItem preferredItem;
        boolean virtual;
        boolean canonicalLink;
        boolean representsDocument;
        boolean fallback;
        boolean navigationStateful;
        
        
        /**
         * Create a HstLinkResolver instance with the current <code>requestContext</code>. The {@link SiteMount} is taken from this context. If
         * we have a {@link ResolvedSiteMapItem} on the <code>requestContext</code>, we also set this also for the {@link HstLinkResolver} for context aware link rewriting
         * @param node
         * @param resolvedSiteMapItem
         */
        HstLinkResolver(Node node, HstRequestContext requestContext){
            this.node = node;
            // note: the resolvedSiteMapItem can be null
            this.resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
            this.siteMount = requestContext.getResolvedSiteMount().getSiteMount();
        }
        
        /**
         * Create a HstLinkResolver instance with the current context <code>resolvedSiteMapItem</code>. The {@link SiteMount} is taken from this context
         * @param node
         * @param resolvedSiteMapItem
         * @deprecated Use {@link #HstLinkResolver(Node, HstRequestContext)} instead
         */
        HstLinkResolver(Node node, ResolvedSiteMapItem resolvedSiteMapItem){
            this.node = node;
            this.resolvedSiteMapItem = resolvedSiteMapItem;
            this.siteMount = resolvedSiteMapItem.getResolvedSiteMount().getSiteMount();
        }
        
        /**
         * Create a HstLinkResolver instance for creating a link in this <code>SiteMount</code>. We do not take into account the current context from {@link ResolvedSiteMapItem}
         * when creating a {@link HstLinkResolver} through this constructor
         * @param node
         * @param hstSite
         */
        HstLinkResolver(Node node, SiteMount siteMount){
            this.node = node;
            this.siteMount = siteMount;
        }
        
        HstLink resolve(){
            if(siteMount == null) {
                log.warn("Cannot create link when the siteMount is null. Return null");
                return null;
            }
            if(node == null) {
                log.warn("Cannot create link when the jcr node null. Return a page not found link");
                return pageNotFoundLink(siteMount);
            }
            boolean containerResource = false;
            String pathInfo = null;
            boolean postProcess = true;
            
            Node canonicalNode = null;
            
            if(!navigationStateful) {
                // not context relative, so we try to compute a link wrt the canonical location of the jcr node. If the canonical location is null (virtual only nodes)
                // we'll continue with the non canonical node
                canonicalNode = JCRUtilities.getCanonical(node);
            }
            
            try {
                if(node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                    /*
                     * A hippo resource is not needed to be translated through the HstSiteMap but we create a binary link directly
                     */
                    for(LocationResolver resolver : DefaultHstLinkCreator.this.locationResolvers) {
                        if(node.isNodeType(resolver.getNodeType())) {
                            if(siteMount.getHstSite() != null) {
                                resolver.setLocationMapTree(siteMount.getHstSite().getLocationMapTree());
                            }
                            HstLink link = resolver.resolve(node, siteMount);
                            if(link != null) {
                               return link; 
                            } else {
                                log.debug("Location resolved for nodetype '{}' is not able to create link for node '{}'. Try next location resolver", resolver.getNodeType(), node.getPath());
                            }
                        }
                    }
                   
                    log.warn("There is no resolver that can handle a resource of type '{}'. Return do not found link", node.getPrimaryNodeType().getName());
                    
                    return pageNotFoundLink(siteMount);
                } else {
                    if(canonicalNode != null) {
                        node = canonicalNode;
                    } else {
                        virtual = true;
                    }
                    nodePath = node.getPath();
                    if(node.isNodeType(HippoNodeType.NT_FACETSELECT) || node.isNodeType(HippoNodeType.NT_MIRROR)) {
                        node = JCRUtilities.getDeref(node);
                        if( node == null ) {
                            log.warn("Broken content internal link for '{}'. Cannot create a HstLink for it. Return null", nodePath);
                            return pageNotFoundLink(siteMount);
                        }
                    }
        
                    if(node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                            representsDocument = true;
                        } else if(node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                            node = node.getParent();
                            representsDocument = true;
                        } else if (node.getParent().isNodeType(HippoNodeType.NT_FACETRESULT)) {
                            representsDocument = true;
                        } 
                    }
                    
                    nodePath = node.getPath();
                  
                    if(isBinaryLocation(nodePath)) {
                        log.debug("Binary path, return hstLink prefixing this path with '{}'", DefaultHstLinkCreator.this.getBinariesPrefix());
                        // Do not postProcess binary locations, as the BinariesServlet is not aware about preprocessing links
                        pathInfo = DefaultHstLinkCreator.this.getBinariesPrefix()+nodePath;
                        containerResource = true;
                        return new HstLinkImpl(pathInfo, siteMount, containerResource);
                        
                    } else {
                        if(!virtual && nodePath.startsWith(siteMount.getCanonicalContentPath())) {
                            nodePath = nodePath.substring(siteMount.getCanonicalContentPath().length());
                        } else if (virtual && nodePath.startsWith(siteMount.getContentPath())) { 
                            nodePath = nodePath.substring(siteMount.getContentPath().length());
                        } else {
                            log.info("For SiteMount '{}' we cannot create a link for node '{}' because it is outside its scope. We'll try other sitemount's now", siteMount.getName(), nodePath);
                            // TODO try subsites CROSS-SITE/DOMAIN-LINKING!!!
                            return pageNotFoundLink(siteMount);
                        }
                        
                        ResolvedLocationMapTreeItem resolvedLocation = null;
                        if( preferredItem != null) {
                            LocationMapResolver subResolver = getSubLocationMapResolver(preferredItem);
                            subResolver.setRepresentsDocument(representsDocument);
                            subResolver.setResolvedSiteMapItem(resolvedSiteMapItem);
                            subResolver.setCanonical(canonicalLink);
                            subResolver.setSubResolver(true);
                            resolvedLocation = subResolver.resolve(nodePath);
                            if( (resolvedLocation == null || resolvedLocation.getPath() == null) && !fallback) {
                                log.warn("Could not create a link for preferredItem '{}'. Fallback is false, so return a not found link.", preferredItem.getId());
                                return pageNotFoundLink(siteMount);
                            }
                        }
                        if(siteMount.getHstSite() != null) {
                            if(resolvedLocation == null) {
                                LocationMapResolver resolver = new LocationMapResolver(siteMount.getHstSite().getLocationMapTree());
                                resolver.setRepresentsDocument(representsDocument);
                                resolver.setCanonical(canonicalLink);
                                resolver.setResolvedSiteMapItem(resolvedSiteMapItem);
                                resolvedLocation = resolver.resolve(nodePath);
                            }
                            if(resolvedLocation != null && resolvedLocation.getPath() != null) {
                                if (log.isDebugEnabled()) log.debug("Creating a link for node '{}' succeeded", nodePath);
                                if (log.isInfoEnabled()) log.info("Succesfull linkcreation for nodepath '{}' to new path '{}'", nodePath, resolvedLocation.getPath());
                                pathInfo = resolvedLocation.getPath();
                            } else {
                                 if (log.isWarnEnabled()) {
                                    log.warn("Unable to create a link for '{}' for siteMount '{}'. Return page not found HstLink to '"+DefaultHstLinkCreator.this.pageNotFoundPath+"'", nodePath, siteMount.getName());
                                    return pageNotFoundLink(siteMount);
                                }
                            }
                        } else {
                            // the SiteMount does not have a HstSite attached to it. Just use the 'nodePath' we have so far as
                            // we do not have a further SiteMap mapping. We only have a site content base path mapping
                            pathInfo = nodePath;
                        }
                        
                    }
                }
            } catch(RepositoryException e){
                log.error("Repository Exception during creating link", e);
            }
            
            if(pathInfo == null) {
                return pageNotFoundLink(siteMount);
            }
            
            HstLink link = new HstLinkImpl(pathInfo, siteMount, containerResource);
            if(postProcess) {
                link = postProcess(link);
            }
            return link;
            
        }

        
        private HstLink pageNotFoundLink(SiteMount siteMount) {
            HstLink link =  new HstLinkImpl(DefaultHstLinkCreator.this.pageNotFoundPath, siteMount);
            link.setNotFound(true);
            return link;
        }


    }


}
