package org.hippoecm.hst.core.linking;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItemUtitlites;
import org.hippoecm.hst.core.jcr.JCRUtilities;
import org.hippoecm.hst.core.linking.HstPathConvertor.ConversionResult;
import org.hippoecm.hst.service.jcr.JCRService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHstLinkCreator implements HstLinkCreator {

    private static final Logger log = LoggerFactory.getLogger(HstLinkCreator.class);

    public HstLink create(JCRService jcrService, HstSiteMapItem siteMapItem) {
        Node n = jcrService.getValueProvider().getJcrNode();
        if (n != null) {
            return this.create(n, siteMapItem);
        } else {
            log.warn("JCRValueProvider is detached. Trying to create link with detached node path");
            // TODO create link with nodepath
        }

        return null;
    }

    /**
     * If the node is of type hippo:document and it is below a hippo:handle, we will
     * rewrite the link wrt hippo:handle, because a handle is the umbrella of a document
     */
    public HstLink create(Node node, HstSiteMapItem siteMapItem) {
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

            String nodePath = node.getPath();
            
            if(node.isNodeType(HippoNodeType.NT_DOCUMENT) && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                log.debug("Node '{}' is a '{}' that belongs to a handle. Create the link with the handle", nodePath, HippoNodeType.NT_DOCUMENT);
                nodePath = node.getParent().getPath();
            }
                
            // Try to see if we can create a link within the HstSite where this HstSiteMapItem belongs to
            HstPathConvertor hstPathConvertor = new BasicHstPathConvertor();
            HstSiteMap hstSiteMap = siteMapItem.getHstSiteMap();
            HstSite hstSite = hstSiteMap.getSite();
            
            if(nodePath.startsWith(hstSite.getLocationMap().getCanonicalSiteContentPath())) {
                ConversionResult result = hstPathConvertor.convert(nodePath, hstSite);
                if(result != null) {
                    log.debug("Creating a link for node '{}' succeeded", nodePath);
                    log.info("Succesfull linkcreation for nodepath '{}' to new path '{}'", nodePath, result.getPath());
                    return new HstLinkImpl(result.getPath(), hstSite);
                } else {
                  // TODO should we try different HstSites?
                  log.warn("Unable to create a link for '{}' for HstSite '{}'. Return null", nodePath, hstSite.getName());
                      
                }
            } else {
                log.debug("For HstSite '{}' we cannot a create a link for node '{}' because it is outside the site scope", hstSite.getName(), nodePath);
                // TODO try to link to another HstSite that has a matching 'content base path'
            }
            
        } catch (RepositoryException e) {
            log.error("Repository Exception during creating link", e);
        }
        
        
        return null;
    }

    public HstLink create(String toSiteMapItemId, HstSiteMapItem currentSiteMapItem) {
        HstSiteMap hstSiteMap = currentSiteMapItem.getHstSiteMap();
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
            log.warn("No site found with a siteMap containing id '{}'. Cannot create link.", toSiteMapItemId);
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
            log.warn("No sitemap item found for id '{}' within Site '{}'. Cannot create link.", toSiteMapItemId,
                    hstSite.getName());
            return null;
        }

        return new HstLinkImpl(HstSiteMapItemUtitlites.getPath(siteMapItem), hstSite);
    }

    

}
