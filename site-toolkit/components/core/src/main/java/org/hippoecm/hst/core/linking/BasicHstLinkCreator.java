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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.BasicLocationMapTree;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItemUtitlites;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.provider.jcr.JCRUtilities;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.jcr.JCRService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHstLinkCreator implements HstLinkCreator {

    private static final Logger log = LoggerFactory.getLogger(BasicHstLinkCreator.class);

    public HstLink create(Service service, ResolvedSiteMapItem resolvedSiteMapItem) {
        if(service instanceof JCRService){
            return this.create((JCRService)service, resolvedSiteMapItem);
        }
        String path  = service.getValueProvider().getPath();
        return this.create(path, resolvedSiteMapItem, true);
    }

    public HstLink create(JCRService jcrService, ResolvedSiteMapItem resolvedSiteMapItem) {
        JCRValueProvider provider = jcrService.getValueProvider();
        
        if(provider.getHandlePath() != null) {
            return this.create(provider.getHandlePath(), resolvedSiteMapItem, true);
        } else if(provider.getCanonicalPath() != null) {
            return this.create(provider.getCanonicalPath(), resolvedSiteMapItem, true);
        } else {
            return this.create(provider.getPath(), resolvedSiteMapItem, true);
        }
        
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
    
    /*
     * boolean signature is only needed to distinguish from create(String toSiteMapItemId, HstSiteMapItem currentSiteMapItem)
     * and not used
     */
    private HstLink create(String path, ResolvedSiteMapItem resolvedSiteMapItem, boolean representsDocument) {
     // Try to see if we can create a link within the HstSite where this HstSiteMapItem belongs to
        HstSiteMap hstSiteMap = resolvedSiteMapItem.getHstSiteMapItem().getHstSiteMap();
        HstSite hstSite = hstSiteMap.getSite();
        
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

        String path = HstSiteMapItemUtitlites.getPath(toSiteMapItem);

        return new HstLinkImpl(path, hstSiteMap.getSite());
    }

    public HstLink create(HstSiteMapItem toHstSiteMapItem) {
        return new HstLinkImpl(HstSiteMapItemUtitlites.getPath(toHstSiteMapItem), toHstSiteMapItem.getHstSiteMap().getSite());
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

        return new HstLinkImpl(HstSiteMapItemUtitlites.getPath(siteMapItem), hstSite);
    }

    

}
