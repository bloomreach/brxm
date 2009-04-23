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
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.provider.jcr.JCRUtilities;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHstLinkCreator implements HstLinkCreator {

    private static final Logger log = LoggerFactory.getLogger(BasicHstLinkCreator.class);

   
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
                if(bean instanceof HippoDocument) {
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
            if (canonical == null) {
                log.warn("Canonical node not found. Trying to create a link for a virtual node");
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
        
        // Try to see if we can create a link within the HstSite where this HstSiteMapItem belongs to
        HstSiteMap hstSiteMap = resolvedSiteMapItem.getHstSiteMapItem().getHstSiteMap();
        HstSiteService hstSite = (HstSiteService)hstSiteMap.getSite(); 
        
        // TODO make this configurable behavior instead of hardcoded. Also it should work for subsite galleries, see HSTTWO-454
        if(path.startsWith("/content/gallery") || path.startsWith("/content/assets")) {
            log.debug("Binary path, return hstLink prefixing this path with '/binaries'");
            return new HstLinkImpl("binaries"+path, hstSite);
        }
        
        if(hstSite.getLocationMapTree() instanceof BasicLocationMapTree) {
            if(path.startsWith(((BasicLocationMapTree)hstSite.getLocationMapTree()).getCanonicalSiteContentPath())) {
                ResolvedLocationMapTreeItem resolvedLocation = hstSite.getLocationMapTree().match(path, hstSite, representsDocument);
                if(resolvedLocation != null) {
                    if (log.isDebugEnabled()) log.debug("Creating a link for node '{}' succeeded", path);
                    if (log.isInfoEnabled()) log.info("Succesfull linkcreation for nodepath '{}' to new path '{}'", path, resolvedLocation.getPath());
                    return new HstLinkImpl(resolvedLocation.getPath(), hstSite);
                } else {
                    // TODO what to return??
                     if (log.isWarnEnabled()) {
                        log.warn("Unable to create a link for '{}' for HstSite '{}'. Return null", path, hstSite.getName());
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

        return new HstLinkImpl(path, hstSiteMap.getSite());
    }


    public HstLink create(String siteMapPath, HstSite hstSite) {
        return new HstLinkImpl(PathUtils.normalizePath(siteMapPath), hstSite);
    }

    
    public HstLink create(HstSiteMapItem toHstSiteMapItem) {
        return new HstLinkImpl(getPath(toHstSiteMapItem), toHstSiteMapItem.getHstSiteMap().getSite());
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

        return new HstLinkImpl(getPath(siteMapItem), hstSite);
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

}
