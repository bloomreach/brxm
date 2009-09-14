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
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.provider.jcr.JCRUtilities;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHstLinkCreator implements HstLinkCreator {

    private static final Logger log = LoggerFactory.getLogger(BasicHstLinkCreator.class);

    private final static String DEFAULT_PAGE_NOT_FOUND_PATH = "pagenotfound";
    private String[] binaryLocations;
    private String binariesPrefix;
    private String pageNotFoundPath = DEFAULT_PAGE_NOT_FOUND_PATH;
    private HstLinkProcessor linkProcessor;
   
    public void setBinariesPrefix(String binariesPrefix){
        this.binariesPrefix = PathUtils.normalizePath(binariesPrefix);
    }
    
    public void setBinaryLocations(String[] binaryLocations) {
        this.binaryLocations = binaryLocations;
    }
    
    public void setlinkProcessor(HstLinkProcessor linkProcessor) {
        this.linkProcessor = linkProcessor;
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
        if(bean.getNode() == null) {
            log.debug("Jcr node is detached from bean. Trying to create a link with the detached path.");
            if(bean.getPath() != null) {
                if(bean instanceof HippoDocumentBean) {
                    return this.create(bean.getPath(), hstRequestContext.getResolvedSiteMapItem(), true);
                } else {
                    return this.create(bean.getPath(), hstRequestContext.getResolvedSiteMapItem(), false);
                }
            } else {
                log.warn("Cannot create link for bean. Return null");
            }
            return null;
        }
        return this.create(bean.getNode(), hstRequestContext.getResolvedSiteMapItem());
    }

    /**
     * If the node is of type hippo:document and it is below a hippo:handle, we will
     * rewrite the link wrt hippo:handle, because a handle is the umbrella of a document
     */
    public HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem) {
        // TODO link creation involves many jcr calls. Cache result possibly with as key hippo:uuid in case of virtual node, and 
        // with jcr:uuid in case of normal referenceable node. This has a lightweight lookup.
        
        try {
            Node canonical = JCRUtilities.getCanonical(node);
            
            if(node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                
                // now we test if this is a resource linked to from a document and is located below /content/gallery or /content/images
                
                if (canonical == null) {
                    log.warn("Canonical cannot be null for hippo:resource. Cannot create a link for '{}'. Return null.", node.getPath());
                    return null;
                }
                
                if(!isBinaryLocation(canonical.getPath())) {
                    /*
                     * A hippo resource is not needed to be translated through the HstSiteMap but we create a binary link directly
                     * Note that this is a Hippo Resource *in* a document outside /content/assets or /content/images. This means, that
                     * you have a context aware resource: The live view is only allowed to view it when it is published for example. Therefor
                     * we do not fallback to the canonical node, but return the link in context.
                     *
                     */ 
                    // Do not postProcess binary locations, as the BinariesServlet is not aware about preprocessing links
                    return new HstLinkImpl(this.getBinariesPrefix()+node.getPath(), getHstSite(resolvedSiteMapItem));
                
                }
                // if we get here, do normal linkrewriting for binary wrt canonical: it was linked to from /content/assets or /content/images
                return new HstLinkImpl(this.getBinariesPrefix()+canonical.getPath(), getHstSite(resolvedSiteMapItem));
            }
            
            if (canonical == null) {
                log.debug("Canonical node not found. Trying to create a link for a virtual node");
            } else if (!canonical.isSame(node)) {
                log.debug("Trying to create link for the canonical equivalence of the node. ('{}' --> '{}')", node.getPath(), canonical.getPath());
                node = canonical;
                if(node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    Node deref = JCRUtilities.getDeref(node);
                    if(deref != null) {
                        log.debug("Node was a facetselect. Creating link for the dereferenced node.");
                        node = deref;
                    }
                }
            } else if(node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                Node deref = JCRUtilities.getDeref(node);
                if(deref != null) {
                    log.debug("Node was a facetselect. Creating link for the dereferenced node.");
                    node = deref;
                }
            } else {
                log.debug("Node was not virtual");
            }

            if(node.isNodeType(HippoNodeType.NT_DOCUMENT) && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                node = node.getParent();
            }
            
            String nodePath = node.getPath();
            
            boolean representsDocument = false;
            if(node.isNodeType(HippoNodeType.NT_HANDLE)) {
                representsDocument = true;
            }
            return this.create(nodePath, resolvedSiteMapItem, representsDocument);
            
        } catch (RepositoryException e) {
            log.error("Repository Exception during creating link", e);
        }
        
        return null;
    }
    
    private HstLink create(String path, ResolvedSiteMapItem resolvedSiteMapItem, boolean representsDocument) {
        
        if(path == null) {
            log.warn("Cannot create HstLink for path null");
            return null;
        }
        
        HstSiteService hstSite = getHstSite(resolvedSiteMapItem);
        
        if(isBinaryLocation(path)) {
            log.debug("Binary path, return hstLink prefixing this path with '{}'", this.getBinariesPrefix());
            // Do not postProcess binary locations, as the BinariesServlet is not aware about preprocessing links
            return new HstLinkImpl(this.getBinariesPrefix()+path, hstSite);
        }
        
        if(hstSite.getLocationMapTree() instanceof BasicLocationMapTree) {
            if(path.startsWith(((BasicLocationMapTree)hstSite.getLocationMapTree()).getCanonicalSiteContentPath())) {
                ResolvedLocationMapTreeItem resolvedLocation = hstSite.getLocationMapTree().match(path, hstSite, representsDocument);
                if(resolvedLocation != null && resolvedLocation.getPath() != null) {
                    if (log.isDebugEnabled()) log.debug("Creating a link for node '{}' succeeded", path);
                    if (log.isInfoEnabled()) log.info("Succesfull linkcreation for nodepath '{}' to new path '{}'", path, resolvedLocation.getPath());
                    return postProcess(new HstLinkImpl(resolvedLocation.getPath(), hstSite));
                } else {
                     if (log.isWarnEnabled()) {
                        String msg = "";
                        if(resolvedLocation != null) {
                            msg = " We cannot create a pathInfo for resolved sitemap item : '" +resolvedLocation.getHstSiteMapItemId() +"'."   ;
                        }
                        log.warn("Unable to create a link for '{}' for HstSite '{}'. " +msg+ "  Return page not found HstLink to '"+this.pageNotFoundPath+"'", path, hstSite.getName());
                        HstLink link =  new HstLinkImpl(pageNotFoundPath, hstSite);
                        link.setNotFound(true);
                        return link;
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("For HstSite '{}' we cannot create a link for node '{}' because it is outside the site scope", hstSite.getName(), path);
                }
                // TODO try to link to another HstSite that has a matching 'content base path'
            }
        }
        return null;
    }
    
    private HstSiteService getHstSite(ResolvedSiteMapItem resolvedSiteMapItem) {
        HstSiteMap hstSiteMap = resolvedSiteMapItem.getHstSiteMapItem().getHstSiteMap();
        return (HstSiteService)hstSiteMap.getSite(); 
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


    public HstLink create(String path, HstSite hstSite) {
        return postProcess(new HstLinkImpl(PathUtils.normalizePath(path), hstSite));
    }
    
    public HstLink create(String path, HstSite hstSite, boolean containerResource) {
        return postProcess(new HstLinkImpl(PathUtils.normalizePath(path), hstSite, containerResource));
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

}
