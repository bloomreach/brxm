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

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSiteService;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
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
     */
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
   
    public HstLink create(Node node, HstRequestContext hstRequestContext) {
        return this.create(node, hstRequestContext.getResolvedSiteMapItem());
    }

    public HstLink create(HippoBean bean, HstRequestContext hstRequestContext) {
        return create(bean.getNode(), hstRequestContext.getResolvedSiteMapItem());
    }
    
    public HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, resolvedSiteMapItem);
        return linkResolver.resolve();
    }
    

    public HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem,
            boolean fallback) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, resolvedSiteMapItem);
        linkResolver.preferredItem = preferredItem;
        linkResolver.fallback = fallback;
        return linkResolver.resolve();
    }
    
    public HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem) {
        return this.createCanonical(node, resolvedSiteMapItem, null);
    }
    
    public HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, resolvedSiteMapItem);
        linkResolver.canonicalLink = true;
        linkResolver.preferredItem = preferredItem;
        // when no canonical can be found for the preferred item, we fallback to linkrewriting without the canonical 
        linkResolver.fallback = true;
        return linkResolver.resolve();
    }
    

    public HstLink create(Node node, HstSite hstSite) {
        if(!(hstSite instanceof HstSiteService)) {
            throw new IllegalArgumentException("hstSite must be an instance of HstSiteService");
        }
        HstLinkResolver linkResolver = new HstLinkResolver(node, (HstSiteService)hstSite);
        return linkResolver.resolve();
    }

    /**
     * TODO we still need to implement this method. Currently, we throw an UnsupportedOperationException, see HSTTWO-1054
     * {@inheritDoc} 
     */
    public HstLink create(Node node, HstSites hstSites) {
        throw new UnsupportedOperationException("This method is not yet supported. See HSTTWO-1054");
    }


    public HstLink create(String path, HstSite hstSite) {
        return postProcess(new HstLinkImpl(PathUtils.normalizePath(path), hstSite));
    }
    
    public HstLink create(String path, HstSite hstSite, boolean containerResource) {
        return postProcess(new HstLinkImpl(PathUtils.normalizePath(path), hstSite, containerResource));
    }

    public HstLink create(String toSiteMapItemId, ResolvedSiteMapItem currentSiteMapItem) {
        HstSiteMap hstSiteMap = currentSiteMapItem.getHstSiteMapItem().getHstSiteMap();
        HstSiteMapItem toSiteMapItem = hstSiteMap.getSiteMapItemById(toSiteMapItemId);
        if (toSiteMapItem == null) {
            // search in different sites
            HstSites hstSites = hstSiteMap.getSite().getHstSites();
            for (HstSite site : hstSites.getSites().values()) {
                toSiteMapItem = site.getSiteMap().getSiteMapItemById(toSiteMapItemId);
                if (toSiteMapItem != null) {
                    log.debug("SiteMapItemId found in different Site. Create link to different site");
                    break;
                }
            }
        }
        if (toSiteMapItem == null) {
            if (log.isWarnEnabled()) {
                log.warn("No site found with a siteMap containing id '{}'. Cannot create link.", toSiteMapItemId);
            }
            return null;
        }
        String path = getPath(toSiteMapItem);
        return postProcess(new HstLinkImpl(path, hstSiteMap.getSite()));
    }
    
    public HstLink create(HstSiteMapItem toHstSiteMapItem) {
        return postProcess(new HstLinkImpl(getPath(toHstSiteMapItem), toHstSiteMapItem.getHstSiteMap().getSite()));
    }

    public HstLink create(HstSite hstSite, String toSiteMapItemId) {
        HstSiteMapItem siteMapItem = hstSite.getSiteMap().getSiteMapItemById(toSiteMapItemId);

        if (siteMapItem == null) {
            if (log.isWarnEnabled()) {
                log.warn("No sitemap item found for id '{}' within Site '{}'. Cannot create link.", toSiteMapItemId,
                        hstSite.getName());
            }
            return null;
        }
        
        return postProcess(new HstLinkImpl(getPath(siteMapItem), hstSite));
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
        HstSiteService hstSite;
        
        HstSiteMapItem preferredItem;
        boolean onlyVirtual;
        boolean canonicalLink;
        boolean representsDocument;
        boolean fallback;
        
        
        /**
         * Create a HstLinkResolver instance with the current context <code>resolvedSiteMapItem</code>. The {@link HstSite} is taken from this context
         * @param node
         * @param resolvedSiteMapItem
         */
        HstLinkResolver(Node node, ResolvedSiteMapItem resolvedSiteMapItem){
            this.node = node;
            this.resolvedSiteMapItem = resolvedSiteMapItem;
            HstSiteMap hstSiteMap = resolvedSiteMapItem.getHstSiteMapItem().getHstSiteMap();
            hstSite =  (HstSiteService)hstSiteMap.getSite(); 
        }
        
        /**
         * Create a HstLinkResolver instance for creating a link in this <code>hstSite</code>. We do not take into account the current context from {@link ResolvedSiteMapItem}
         * when creating a {@link HstLinkResolver} through this constructor
         * @param node
         * @param hstSite
         */
        HstLinkResolver(Node node, HstSiteService hstSite){
            this.node = node;
            this.hstSite = hstSite;
        }
        
        HstLink resolve(){
            if(node == null) {
                log.warn("Cannot create link for bean. Return page not found link");
                return pageNotFoundLink(hstSite);
            }
            boolean containerResource = false;
            String pathInfo = null;
            boolean postProcess = true;
            
            Node canonicalNode = JCRUtilities.getCanonical(node);
           
            try {
                if(node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                    /*
                     * A hippo resource is not needed to be translated through the HstSiteMap but we create a binary link directly
                     */
                    for(LocationResolver resolver : DefaultHstLinkCreator.this.locationResolvers) {
                        if(node.isNodeType(resolver.getNodeType())) {
                            resolver.setLocationMapTree(hstSite.getLocationMapTree());
                            HstLink link = resolver.resolve(node, hstSite);
                            if(link != null) {
                               return link; 
                            } else {
                                log.debug("Location resolved for nodetype '{}' is not able to create link for node '{}'. Try next location resolver", resolver.getNodeType(), node.getPath());
                            }
                        }
                    }
                   
                    log.warn("There is no resolver that can handle a resource of type '{}'. Return do not found link", node.getPrimaryNodeType().getName());
                    
                    return pageNotFoundLink(hstSite);
                } else {
                    if(canonicalNode != null) {
                        node = canonicalNode;
                    } else {
                        onlyVirtual = true;
                    }
                    nodePath = node.getPath();
                    if(node.isNodeType(HippoNodeType.NT_FACETSELECT) || node.isNodeType(HippoNodeType.NT_MIRROR)) {
                        node = JCRUtilities.getDeref(node);
                        if( node == null ) {
                            log.warn("Broken content internal link for '{}'. Cannot create a HstLink for it. Return null", nodePath);
                            return null;
                        }
                    }
        
                    if(node.isNodeType(HippoNodeType.NT_DOCUMENT) && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                        node = node.getParent();
                    }
                    
                    nodePath = node.getPath();
                    if(node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        representsDocument = true;
                    }
                    
                    if(isBinaryLocation(nodePath)) {
                        log.debug("Binary path, return hstLink prefixing this path with '{}'", DefaultHstLinkCreator.this.getBinariesPrefix());
                        // Do not postProcess binary locations, as the BinariesServlet is not aware about preprocessing links
                        pathInfo = DefaultHstLinkCreator.this.getBinariesPrefix()+nodePath;
                        containerResource = true;
                        return new HstLinkImpl(pathInfo, hstSite, containerResource);
                        
                    } else {
                        if(!onlyVirtual && nodePath.startsWith(hstSite.getCanonicalContentPath())) {
                            nodePath = nodePath.substring(hstSite.getCanonicalContentPath().length());
                        } else if (onlyVirtual && nodePath.startsWith(hstSite.getContentPath())) { 
                            nodePath = nodePath.substring(hstSite.getContentPath().length());
                        } else {
                            log.warn("For HstSite '{}' we cannot create a link for node '{}' because it is outside the site scope", hstSite.getName(), nodePath);
                            // TODO try subsites
                            return pageNotFoundLink(hstSite);
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
                                return pageNotFoundLink(hstSite);
                            }
                        }
                        
                        if(resolvedLocation == null) {
                            LocationMapResolver resolver = new LocationMapResolver(hstSite.getLocationMapTree());
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
                                String msg = "";
                                if(resolvedLocation != null) {
                                    msg = " We cannot create a pathInfo for resolved sitemap item : '" +resolvedLocation.getHstSiteMapItemId() +"'."   ;
                                }
                                log.warn("Unable to create a link for '{}' for HstSite '{}'. " +msg+ "  Return page not found HstLink to '"+DefaultHstLinkCreator.this.pageNotFoundPath+"'", nodePath, hstSite.getName());
                                return pageNotFoundLink(hstSite);
                            }
                        }
                    }
                }
            } catch(RepositoryException e){
                log.error("Repository Exception during creating link", e);
            }
            
            if(pathInfo == null) {
                return pageNotFoundLink(hstSite);
            }
            
            HstLink link = new HstLinkImpl(pathInfo, hstSite, containerResource);
            if(postProcess) {
                link = postProcess(link);
            }
            return link;
            
        }

        
        private HstLink pageNotFoundLink(HstSiteService hstSite) {
            HstLink link =  new HstLinkImpl(DefaultHstLinkCreator.this.pageNotFoundPath, hstSite);
            link.setNotFound(true);
            return link;
        }


    }


}
