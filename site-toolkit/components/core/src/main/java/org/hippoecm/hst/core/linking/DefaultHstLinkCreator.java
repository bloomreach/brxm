/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.hst.util.NodeUtils;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.util.WeakIdentityMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.VIRTUALHOST_PROPERTY_CDN_HOST;

public class DefaultHstLinkCreator implements HstLinkCreator {

    private static final Logger log = LoggerFactory.getLogger(DefaultHstLinkCreator.class);

    private static final String DEFAULT_PAGE_NOT_FOUND_PATH = "pagenotfound";
    public static final String BINARIES_PREFIX = "/binaries";
    public static final String BINARIES_START_PATH = "binaries/";
    private String[] binaryLocations;
    private String pageNotFoundPath = DEFAULT_PAGE_NOT_FOUND_PATH;
    private WeakIdentityMap<HstSiteMapItem, SubLocationMapTreesHolder> loadedSubLocationMapTreesHolder = WeakIdentityMap.newConcurrentHashMap();
    private HstLinkProcessor linkProcessor;
    
    private List<LocationResolver> locationResolvers;

    private RewriteContextResolver rewriteContextResolver;

    public void setRewriteContextResolver(RewriteContextResolver rewriteContextResolver) {
        this.rewriteContextResolver = rewriteContextResolver;
    }

    public void setBinaryLocations(String[] binaryLocations) {
        if (binaryLocations == null) {
            this.binaryLocations = null;
        } else {
            this.binaryLocations = new String[binaryLocations.length];
            System.arraycopy(binaryLocations, 0, this.binaryLocations, 0, binaryLocations.length);
        }
    }
    
    public void setLinkProcessor(HstLinkProcessor linkProcessor) {
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

    public void clear() {
        // nothing to clear for now
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
            Node node = session.getNodeByIdentifier(uuid);
            return create(node, requestContext);
        } catch (ItemNotFoundException e) {
            log.info("Node with uuid '{}' cannot be found. Cannot create a HstLink, return null", uuid);
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

    public List<HstLink> createAll(final Node node, final HstRequestContext hstRequestContext, final boolean crossMount) {
        final Mount mount = hstRequestContext.getResolvedMount().getMount();
        final String type = mount.getType();
        final String hostGroupName = mount.getVirtualHost().getHostGroupName();
        return createAll(node, hstRequestContext, hostGroupName, type, crossMount);
    }

    public List<HstLink> createAll(final Node node, final HstRequestContext hstRequestContext,
                                   final String hostGroupName, final String type, final boolean crossMount) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, hstRequestContext);
        if (!crossMount) {
            linkResolver.resolverProperties.tryOtherMounts = false;
        }
        return linkResolver.resolveAll(type, hostGroupName);
    }

    public HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem,
            boolean fallback) {
        return this.create(node, requestContext, preferredItem, fallback, false);
    }
    
    public HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem,
            boolean fallback, boolean navigationStateful) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, requestContext);
        linkResolver.resolverProperties.preferredItem = preferredItem;
        linkResolver.resolverProperties.fallback = fallback;
        if(!fallback) {
            // when preferredItem and fallback is false, we also do not try other mounts
            linkResolver.resolverProperties.tryOtherMounts = false;
        }
        linkResolver.resolverProperties.navigationStateful = navigationStateful;
        return linkResolver.resolve();
    }
    
    public HstLink createCanonical(Node node, HstRequestContext requestContext) {
        return this.createCanonical(node, requestContext, null);
    }

    public HstLink createCanonical(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, requestContext);
        linkResolver.resolverProperties.canonicalLink = true;
        linkResolver.resolverProperties.preferredItem = preferredItem;
        // when no canonical can be found for the preferred item, we fallback to linkrewriting without the canonical 
        linkResolver.resolverProperties.fallback = true;
        return linkResolver.resolve();
    }
    

    @Override
    public List<HstLink> createAllAvailableCanonicals(Node node, HstRequestContext requestContext) {
        return createAllAvailableCanonicals(node, requestContext, null, null);
        
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(Node node, HstRequestContext requestContext, String type) {
        return createAllAvailableCanonicals(node, requestContext, type, null);
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(Node node, HstRequestContext requestContext, String type, String hostGroupName) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, requestContext);
        return linkResolver.resolveAllCanonicals(type, hostGroupName);
    }

    @Override
    public HstLink create(Node node, Mount mount) {
        return create(node, mount, false);
    }

    @Override
    public HstLink create(final Node node, final Mount mount, final boolean crossMount) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, mount);
        linkResolver.resolverProperties.tryOtherMounts = crossMount;
        // when linking to a mount, we always want get a canonical link:
        linkResolver.resolverProperties.canonicalLink = true;
        return linkResolver.resolve();
    }

    @Override
    public HstLink create(final Node node, final Mount mount, final HstSiteMapItem preferredItem, final boolean fallback) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, mount);
        linkResolver.resolverProperties.tryOtherMounts = false;
        linkResolver.resolverProperties.preferredItem = preferredItem;
        linkResolver.resolverProperties.fallback = fallback;
        return linkResolver.resolve();
    }

    public HstLink create(Node node, HstRequestContext requestContext,  String mountAlias) {
        Mount targetMount = requestContext.getMount(mountAlias);
        if(targetMount == null) {
            Mount currentMount = requestContext.getResolvedMount().getMount();
            StringBuffer types = new StringBuffer();
            for(String type: currentMount.getTypes()) {
                if(types.length() > 0) {
                    types.append(",");
                }
                types.append(type);
            }
            String[] messages = {mountAlias , currentMount.getVirtualHost().getHostGroupName(), types.toString()};
            log.info("Cannot create a link for mountAlias '{}' as it cannot be found in the host group '{}' and one of the types '{}'", messages);
            return null;
        }
        
        log.debug("Target Mount found for mountAlias '{}'. Create link for target Mount", mountAlias);
        return create(node, targetMount);
    }


    public HstLink create(Node node, HstRequestContext requestContext,  String mountAlias, String type) {
        Mount targetMount = requestContext.getMount(mountAlias, type);
        if(targetMount == null) {
            String[] messages = {mountAlias , requestContext.getVirtualHost().getHostGroupName(), type};
            log.info("Cannot create a link for mountAlias '{}' as it cannot be found in the host group '{}' for type '{}'", messages);
            return null;
        }
        log.debug("Target Mount found for mountAlias '{}'. Create link for target Mount", mountAlias);
        return create(node, targetMount);
    }

    public HstLink create(final String path, final Mount mount) {
        final String normalizedPath = PathUtils.normalizePath(path);
        HstLink hstLink = null;
        if (normalizedPath.startsWith(BINARIES_START_PATH)) {
            String nodePath = normalizedPath.substring(BINARIES_START_PATH.length() -1);
            if (isBinaryLocation(nodePath)) {
                hstLink = createHstLinkForBinaryLocation(nodePath, mount);
            }
        }
        if (hstLink == null) {
            hstLink = new HstLinkImpl(normalizedPath, mount);
        }
        return postProcess(hstLink);
    }



    public HstLink create(String path, Mount mount, boolean containerResource) {
        final String normalizedPath = PathUtils.normalizePath(path);
        HstLink hstLink = null;
        if (normalizedPath.startsWith(BINARIES_START_PATH)) {
            String nodePath = normalizedPath.substring(BINARIES_START_PATH.length() -1);
            if (isBinaryLocation(nodePath)) {
                hstLink = createHstLinkForBinaryLocation(nodePath, mount);
            }
        }
        if (hstLink == null) {
            hstLink = new HstLinkImpl(normalizedPath, mount, containerResource);
        }
        return postProcess(hstLink);
    }

    private HstLink createHstLinkForBinaryLocation(final String nodePath, final Mount mount) {
        try {
            final Node node = RequestContextProvider.get().getSession().getNode(nodePath);
            Node resourceNode = null;
            Node resourceContainerNode = null;
            if (node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                resourceNode = node;
            } else if (node.isNodeType(HippoNodeType.NT_HANDLE)){
                resourceContainerNode = node.getNode(node.getName());
            } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT) &&  node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                resourceContainerNode = node;
            }
            if (resourceNode == null && resourceContainerNode == null) {
                if (nodePath.startsWith("/")) {
                    return new HstLinkImpl(BINARIES_PREFIX + nodePath, mount, true);
                } else {
                    return new HstLinkImpl(BINARIES_PREFIX + "/" + nodePath, mount, true);
                }
            }

            if (resourceNode != null) {
                for (LocationResolver resolver : locationResolvers) {
                    if (resourceNode.isNodeType(resolver.getNodeType())) {
                        HstLink hstLink = resolver.resolve(resourceNode, mount, mount.getHstSite().getLocationMapTree());
                        if (hstLink != null) {
                            log.debug("Location resolved for nodetype '{}' is able to create link for node '{}'.",
                                    resolver.getNodeType(), nodePath);
                            return hstLink;
                        } else {
                            log.debug("Location resolved for nodetype '{}' is not able to create link for node '{}'. Try next location resolver",
                                    resolver.getNodeType(), nodePath);
                        }
                    }
                }
            } else if (resourceContainerNode != null) {
                // find the primary resource item
                for (LocationResolver resolver : locationResolvers) {
                    if (!(resolver instanceof ResourceLocationResolver)) {
                        continue;
                    }
                    for (ResourceContainer container : ((ResourceLocationResolver)resolver).getResourceContainers()) {
                        if (resourceContainerNode.isNodeType(container.getNodeType()) && container.getPrimaryItem() != null) {
                            if (resourceContainerNode.hasNode(container.getPrimaryItem())) {
                                resourceNode = resourceContainerNode.getNode(container.getPrimaryItem());
                                final String pathInfo = container.resolveToPathInfo(resourceContainerNode, resourceNode, mount);
                                if (pathInfo != null) {
                                    log.debug("Resource Container resolved for nodetype '{}' is able to create link for node '{}'.",
                                            container.getNodeType(), nodePath);
                                    if (pathInfo.startsWith("/")) {
                                        return new HstLinkImpl(BINARIES_PREFIX + pathInfo, mount, true);
                                    } else {
                                        return new HstLinkImpl(BINARIES_PREFIX + "/" + pathInfo, mount, true);
                                    }
                                }
                            }
                            log.debug("resourceContainer for '{}' unable to create a HstLink for path '{}'. Try next", container.getNodeType(), nodePath);
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.debug("Could not find '{}' node for '{}'. Return plain path link.", HippoNodeType.NT_RESOURCE, nodePath);
        }
        if (nodePath.startsWith("/")) {
            return new HstLinkImpl(BINARIES_PREFIX + nodePath, mount, true);
        } else {
            return new HstLinkImpl(BINARIES_PREFIX + "/" + nodePath, mount, true);
        }
    }

    @Override
    public HstLink createPageNotFoundLink(Mount mount) {
        HstLink link = new HstLinkImpl(pageNotFoundPath, mount);
        link.setNotFound(true);
        return link;

    }

    public HstLink create(HstSiteMapItem toHstSiteMapItem, Mount mount) {
        return postProcess(new HstLinkImpl(HstSiteMapUtils.getPath(toHstSiteMapItem), mount));
    }

    public HstLink createByRefId(String siteMapItemRefId, Mount mount) {
        if(mount.getHstSite() == null) {
            log.info("Cannot create a link to a siteMapItemRefId '{}' for a mount '{}' that does not have a HstSiteMap. Return null", siteMapItemRefId, mount.getName());
            return null;
        }
        HstSiteMapItem siteMapItem = mount.getHstSite().getSiteMap().getSiteMapItemByRefId(siteMapItemRefId);
        if(siteMapItem == null) {
            log.info("Could not find HstSiteMapItem for siteMapItemRefId '{}' and mount '{}'. Return null", siteMapItemRefId, mount.getName());
            return null;
        }
        return create(siteMapItem, mount);
    }

    private HstLink postProcess(HstLink link) {
        if(linkProcessor != null) {
            link = linkProcessor.postProcess(link);
        }
        return link;
    }
    
    public String getBinariesPrefix() {
        return BINARIES_PREFIX;
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

    private RewriteContext createRewriteContext(final Node node, final Mount mount,
                              final ResolverProperties resolverProperties) throws RepositoryException, RewriteContextException {

        if (rewriteContextResolver == null) {
            new RewriteContext(node.getPath(), mount, resolverProperties.canonicalLink, resolverProperties.navigationStateful);
        }
        final RewriteContext rewriteContext;
        if (log.isDebugEnabled()) {
            long start = System.nanoTime();
            rewriteContext = rewriteContextResolver.resolve(node, mount, RequestContextProvider.get(),
                    resolverProperties.canonicalLink, resolverProperties.navigationStateful);
            log.debug("RewriteContextResolver '{}' took '{}' ms to get link rewrite path '{}' for node '{}'",
                    rewriteContextResolver.getClass().getName(), String.valueOf((System.nanoTime() - start) / 1000000D),
                    rewriteContext.getPath(), node.getPath());
        } else {
            rewriteContext = rewriteContextResolver.resolve(node, mount, RequestContextProvider.get(),
                    resolverProperties.canonicalLink, resolverProperties.navigationStateful);
        }
        if (rewriteContext.isCanonical() != resolverProperties.canonicalLink) {
            log.debug("Resetting canonical from resolver properties to '{}' because modified through RewriteContext '{}'",
                    rewriteContext.isCanonical(), rewriteContext);
            resolverProperties.canonicalLink = rewriteContext.isCanonical();
        }
        if (rewriteContext.isNavigationStateful() != resolverProperties.navigationStateful) {
            log.debug("Resetting navigationStateful from resolver properties to '{}' because modified through RewriteContext '{}'",
                    rewriteContext.isNavigationStateful(), rewriteContext);
            resolverProperties.navigationStateful = rewriteContext.isNavigationStateful();
        }
        return rewriteContext;
    }

    private class HstLinkResolver {
        
        Node node;
        final String originalNodePath;
        Mount mount;
        ResolverProperties resolverProperties;

        /**
         * Create a HstLinkResolver instance with the current <code>requestContext</code>. The {@link Mount} is taken from this context. If
         * we have a {@link ResolvedSiteMapItem} on the <code>requestContext</code>, we also set this also for the {@link HstLinkResolver} for context aware link rewriting
         * @param requestContext
         * @param node
         */
        HstLinkResolver(Node node, HstRequestContext requestContext){
            this.node = node;
            originalNodePath = getOriginalNodePath(node);
            // note: the resolvedSiteMapItem can be null
            resolverProperties = new ResolverProperties();
            resolverProperties.resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
            mount = requestContext.getResolvedMount().getMount();
        }

        /**
         * Create a HstLinkResolver instance for creating a link in this {@link Mount}. We do not take into account the current context from {@link ResolvedSiteMapItem}
         * when creating a {@link HstLinkResolver} through this constructor
         * @param node
         * @param mount
         */
        HstLinkResolver(Node node, Mount mount){
            this.node = node;
            originalNodePath = getOriginalNodePath(node);
            this.mount = mount;
            resolverProperties = new ResolverProperties();
        }

        private String getOriginalNodePath(final Node node) {
            try {
                return node.getPath();
            } catch (RepositoryException e) {
                log.error("Repository exception while fetching node path");
                return "??";
            }
        }


        protected HstLink resolve() {
            if(mount == null) {
                log.info("Cannot create link when the mount is null. Return null");
                return null;
            }
            if(node == null) {
                log.info("Cannot create link when the jcr node null. Return a page not found link");
                return createPageNotFoundLink(mount);
            }

            Node canonicalNode = null;
            if(!resolverProperties.navigationStateful) {
                // not context relative, so we try to compute a link wrt the canonical location of the jcr node. If the canonical location is null (virtual only nodes)
                // we'll continue with the non canonical node
                canonicalNode = NodeUtils.getCanonicalNode(node);
            }

            HstLink hstLink = null;
            try {
                if(node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                    /*
                     * A hippo resource is not needed to be translated through the HstSiteMap but we create a binary link directly
                     */
                    for(LocationResolver resolver : locationResolvers) {
                        if(node.isNodeType(resolver.getNodeType())) {

                            final LocationMapTree locationMapTree;
                            if (mount.isMapped() && mount.getHstSite() != null) {
                                locationMapTree = mount.getHstSite().getLocationMapTree();
                            } else {
                                locationMapTree = null;
                            }
                            
                            HstLink link = resolver.resolve(node, mount, locationMapTree);
                            if(link != null) {
                               return link;
                            } else {
                                log.debug("Location resolved for nodetype '{}' is not able to create link for node '{}'. Try next location resolver",
                                        resolver.getNodeType(), originalNodePath);
                            }
                        }
                    }

                    log.info("There is no resolver that can handle a resource of type '{}'. Return do not found link", node.getPrimaryNodeType().getName());
                    return createPageNotFoundLink(mount);
                } else {
                    if (canonicalNode != null) {
                        node = canonicalNode;
                    } else {
                        resolverProperties.virtual = true;
                    }
                    if (node.isNodeType(HippoNodeType.NT_FACETSELECT) || node.isNodeType(HippoNodeType.NT_MIRROR)) {
                        node = NodeUtils.getDeref(node);
                        if (node == null) {
                            log.info("Broken content internal link for '{}'. Cannot create a HstLink for it. Return null", originalNodePath);
                            return createPageNotFoundLink(mount);
                        }
                    }

                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        resolverProperties.representsDocument = true;
                    } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        if (node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                            node = node.getParent();
                            resolverProperties.representsDocument = true;
                        } else if (node.getParent().isNodeType(HippoNodeType.NT_FACETRESULT)) {
                            resolverProperties.representsDocument = true;
                        }
                    }

                    final RewriteContext rewriteContext = createRewriteContext(node, mount, resolverProperties);

                    final String rewritePath = rewriteContext.getPath();
                    final Mount rewriteMount = rewriteContext.getMount();

                    hstLink = resolveToHstLink(rewritePath, rewriteMount, resolverProperties);

                    if (hstLink == null && resolverProperties.tryOtherMounts) {

                        log.debug("We cannot create a link for '{}' for the mount '{}' belonging to the current request. Try to create a cross-domain/site/channel link.",
                                rewritePath, rewriteMount.getName());

                        // when trying other mounts, we do not support 'preferredItem'. Set to null if it wasn't already set to null

                        if(resolverProperties.preferredItem != null) {
                            // cannot use preferredItem and cross domain linking at same time. We set it to null
                            resolverProperties.preferredItem = null;
                            log.info("Trying other mount than current context mount for nodePath '{}'. Cross domain linking cannot be combined with linking to a preferred sitemap item. " +
                                    "We'll ignore the preferred item," , rewritePath);
                        }
                        
                        /*
                         * The Mount with which this HstLinkResolver was created can not be used to create a link for the nodePath because the path
                         * is out of the scope of the (sub)site or the sitemap of the (sub)site was unable to create a link for it. We'll now try to find a 
                         * Mount that can create a link for it. If there is no Mount that can create a link, a pagenotfound link is created with the original mount.
                         * 
                         * Note that we only create a cross-domain link if and only if there is a Mount that 
                         * 1) Has a #getContentPath() (for virtual nodes) that start
                         * with or are equal to the 'nodePath'
                         * 2) Belong to the same HostNameGroup as the Mount for this HstLinkResolver (normally the same as for the current request)
                         * 3) Has at least one type (preview, live, composer, etc ) in common with the Mount for this HstLinkResolver 
                         *
                         * Note that if there is a preferredItem we ignore this one for cross domain linking as preferredItem only work within the same Mount
                         */

                        List<Mount> candidateMounts = findAllCandidateMounts(rewriteMount, rewritePath,
                                rewriteMount.getVirtualHost().getHostGroupName(), rewriteMount.getType());

                        /** The resulting list of candidate Mounts can all be tried to create a link for until there is a Mount that
                         * returned a non null LinkInfo. The order in which we try the candidate mount for a LinkInfo is as follows:
                         *
                         * 1) Firstly order the candidate mounts to have the same primary type as the current Mount of this HstLinkResolver
                         * 2) Secondly order the candidate mounts that have the most 'types' in common with the current Mount of this HstLinkResolver
                         * 3) Thirdly order the Mounts to have the fewest types first: The fewer types it has, and the number of matching types is equal to the current Mount, indicates
                         * that it can be considered more precise
                         * 4) Fourthly order the Mounts first that have the deepest (most slashes) #getCanonicalContentPath() : The deeper the more specific.
                         */
                        if(candidateMounts.size() == 0) {
                            log.info("There is no Mount available that is suited to linkrewrite '{}'. Return page not found link.", rewritePath);
                            return createPageNotFoundLink(rewriteMount);
                        } else if(candidateMounts.size() == 1) {
                            hstLink = resolveToHstLink(rewritePath, candidateMounts.get(0), resolverProperties);
                        } else {
                            // sort the candidate mounts according the algorithm mount ordering (1), (2), (3) and (4) applied
                            // this is done by the CandidateMountComparator which gets the current 'mount' as reference for
                            // the ordering 

                            Collections.sort(candidateMounts, new CandidateMountComparator(rewriteMount));

                            for(Mount tryMount : candidateMounts) {
                                hstLink = resolveToHstLink(rewritePath, tryMount, resolverProperties);
                                if(hstLink != null) {
                                    // succeeded
                                    break;
                                }
                            }
                        }


                    }
                }

            } catch (RewriteContextException e){
                log.debug("Returning not found link for '{}' :", originalNodePath, e);
                return createPageNotFoundLink(mount);
            } catch(RepositoryException e){
                log.error("Repository Exception during creating link", e);
            } catch (Exception e) {
                log.error("Exception during creating link", e);
            }

            if (hstLink == null) {
                log.info("Cannot create a link for node with path '{}'. Return a page not found link", originalNodePath);
                return createPageNotFoundLink(mount);
            }
            return postProcess(hstLink);
        }

        /**
         * If <code>type</code> is null, the types of the current {@link Mount} are used. If <code>hostGroupName</code> is null, the 
         * hostGroupName of the current {@link Mount} is used. 
         * @param type
         * @param hostGroupName
         * @return the List of all available links. When no links at all are found, an empty list is returned. 
         */
        List<HstLink> resolveAllCanonicals(String type, String hostGroupName) {

            resolverProperties.canonicalLink = true;
            if(resolverProperties.preferredItem != null) {
                log.info("preferredItem is not supported in combination with 'all available canonical links'. It will be ignored");
            }
            if(resolverProperties.navigationStateful) {
                log.info("navigationStateful is not supported in combination with 'all available canonical links'. It will be ignored");
            }

            if (mount == null) {
                log.info("Cannot create link when the mount is null. Return empty list for canonicalLinks.");
                return Collections.emptyList();
            }
            if (node == null) {
                log.info("Cannot create link when the jcr node is null. Return empty list for canonicalLinks.");
                return Collections.emptyList();
            }

            Node canonicalNode = NodeUtils.getCanonicalNode(node);

            List<HstLink> hstLinkList = new ArrayList<>();
            try {
                if(node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                    // we do not support all canonical links for resources (yet)
                    log.info("For binary resources the HST has no support to return all available canonical links");
                    return Collections.emptyList();
                }
                if(canonicalNode == null) {
                    log.debug("The HST has no support to return all available canonical links for virtual only nodes");
                    return Collections.emptyList();
                }

                if(node.isNodeType(HippoNodeType.NT_FACETSELECT) || node.isNodeType(HippoNodeType.NT_MIRROR)) {
                    node = NodeUtils.getDeref(node);
                    if( node == null ) {
                        log.debug("Broken content internal link for '{}'. Cannot create a HstLink for it. Return an empty list for canonical links.", originalNodePath);
                        return Collections.emptyList();
                    }
                }

                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    resolverProperties.representsDocument = true;
                } else if(node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    if(node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                        node = node.getParent();
                        resolverProperties.representsDocument = true;
                    }
                }

                final RewriteContext rewriteContext = createRewriteContext(node, mount, resolverProperties);

                final String rewritePath = rewriteContext.getPath();
                final Mount rewriteMount = rewriteContext.getMount();

                if (type == null) {
                    type = rewriteMount.getType();
                }

                List<Mount> candidateMounts = new ArrayList<>();
                if(hostGroupName == null) {
                    candidateMounts.addAll(findAllCandidateMounts(rewriteMount, rewritePath,
                            rewriteMount.getVirtualHost().getHostGroupName(), type));
                } else {
                    candidateMounts.addAll(findAllCandidateMounts(rewriteMount, rewritePath, hostGroupName, type));
                }

                if(candidateMounts.size() == 0) {
                    log.info("There is no Mount available that is suited to linkrewrite '{}'. Return empty list for canonicalLinks..", rewritePath);
                    return Collections.emptyList();

                }

                for(Mount tryMount : candidateMounts) {
                    HstLink hstLink = resolveToHstLink(rewritePath, tryMount, resolverProperties);
                    if(hstLink != null) {
                        hstLinkList.add(postProcess(hstLink));
                    }
                }

            } catch (RewriteContextException e) {
                log.debug ("LinkPathNotFoundException for '{}'. Return empty list", originalNodePath, e);
                return Collections.emptyList();
            } catch(RepositoryException e){
                log.warn("Repository Exception during creating link", e);
            } catch (Exception e) {
                log.warn("Exception during creating link", e);
            }

            if(hstLinkList.isEmpty()) {
                log.debug("Cannot create any link for node with path '{}'. Return empty list for canonicalLinks.", originalNodePath);
                return Collections.emptyList();
            }

            return hstLinkList;
        }

        List<HstLink> resolveAll(String type, String hostGroupName) {

            if(resolverProperties.preferredItem != null) {
                log.warn("preferredItem is not supported in combination with 'all links'. It will be ignored");
            }
            if(resolverProperties.navigationStateful) {
                log.warn("navigationStateful is not supported in combination with 'all links'. It will be ignored");
            }

            if (mount == null) {
                log.info("Cannot create link when the mount is null. Return empty list for canonicalLinks.");
                return Collections.emptyList();
            }
            if (node == null) {
                log.info("Cannot create link when the jcr node is null. Return empty list for canonicalLinks.");
                return Collections.emptyList();
            }

            final List<HstLink> hstLinkList = new ArrayList<>();
            try {
                if(node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                    // we do not support all canonical links for resources (yet)
                    log.info("For binary resources the HST has no support to return all available canonical links");
                    return Collections.emptyList();
                }

                if (node.isNodeType(HippoNodeType.NT_FACETSELECT) || node.isNodeType(HippoNodeType.NT_MIRROR)) {
                    node = NodeUtils.getDeref(node);
                    if (node == null) {
                        log.debug("Broken content internal link for '{}'. Cannot create a HstLink for it. Return an empty list for canonical links.", originalNodePath);
                        return Collections.emptyList();
                    }
                }

                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    resolverProperties.representsDocument = true;
                } else if(node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    if(node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                        node = node.getParent();
                        resolverProperties.representsDocument = true;
                    }
                }

                final RewriteContext rewriteContext = createRewriteContext(node, mount, resolverProperties);

                final String rewritePath = rewriteContext.getPath();
                final Mount rewriteMount = rewriteContext.getMount();

                if (type == null) {
                    type = rewriteMount.getType();
                }

                // try to get the list of candidateMounts to get a HstLink for

                final List<Mount> candidateMounts = new ArrayList<>();

                if (resolverProperties.tryOtherMounts) {
                    candidateMounts.addAll(findAllCandidateMounts(rewriteMount, rewritePath, hostGroupName, type));
                } else {
                    // only from current context
                    candidateMounts.add(rewriteMount);
                }

                if(candidateMounts.size() == 0) {
                    log.info("There is no Mount available that is suited to linkrewrite '{}'. Return empty list.", rewritePath);
                    return Collections.emptyList();
                }

                for(Mount tryMount : candidateMounts) {
                    List<HstLinkImpl> hstLinkListForMount = resolveToHstLinkList(rewritePath, tryMount, resolverProperties);
                    // strip doubles here.
                    // Two HstLinks with same getPath for one mount are collapsed
                    // Two HstLinks with same getPath except one with extension (/foo and /foo.html) are collapsed to a
                    // one, where in case HstLinkImpl has contentType DOCUMENT the one with extension is kept, and
                    // contentType FOLDER the /foo. If contentType unknown, we keep both

                    Map<String, HstLinkImpl> collapsedLinks = new HashMap<>();
                    for (HstLinkImpl hstLink : hstLinkListForMount) {
                        String path = hstLink.getPath();
                        if (path == null) {
                            continue;
                        }
                        final int index = path.indexOf(".");
                        if (index > -1) {
                            // most likely document
                            String pathBeforeExt = path.substring(0, index);
                            if (collapsedLinks.containsKey(pathBeforeExt)) {
                                // most likely a folder sitemap item. If already exists in collapsed, check the contentType
                                if (hstLink.getContentType() == HstLinkImpl.ContentType.DOCUMENT) {
                                    // replace existing because might be one with .html and we have a folder node
                                    collapsedLinks.put(pathBeforeExt, hstLink);
                                } else {
                                    // nothing because exists already
                                }
                            } else {
                                collapsedLinks.put(path, hstLink);
                            }
                        } else {
                            if (collapsedLinks.containsKey(path)) {
                                // most likely a folder sitemap item. If already exists in collapsed, check the contentType
                                if (hstLink.getContentType() == HstLinkImpl.ContentType.FOLDER) {
                                    // replace existing because might be one with .html and we have a folder node
                                    collapsedLinks.put(path, hstLink);
                                } else {
                                    // nothing because exists already
                                }
                            } else {
                                collapsedLinks.put(path, hstLink);
                            }
                        }
                    }

                    for (HstLink hstLink : collapsedLinks.values()) {
                        hstLinkList.add(postProcess(hstLink));
                    }
                }
            } catch (RewriteContextException e) {
                log.debug ("LinkPathNotFoundException for '{}'. Return empty list", originalNodePath, e);
                return Collections.emptyList();
            } catch(RepositoryException e){
                log.warn("Repository Exception during creating link", e);
            } catch (Exception e) {
                log.warn("Exception during creating link", e);
            }

            if(hstLinkList.isEmpty()) {
                log.debug("Cannot create any link for node with path '{}'. Return empty list for canonicalLinks.", originalNodePath);
                return Collections.emptyList();
            }

            Collections.sort(hstLinkList, new LowestDepthFirstAndThenLexicalComparator());
            return hstLinkList;
        }

        /*
         * There can be multiple suited Mount's (for example the Mount for preview and composermode can be the 'same' subsite). We
         * choose the best suited Mount as follows:
         * a) The Mount must have a #getContentPath() that is a prefix of the nodePath
         * b) The Mount must have at least ONE type in common as the current Mount of this HstLinkResolver
         *
         */
        private List<Mount> findAllCandidateMounts(final Mount rewriteMount, final String rewritePath,
                                                   final String hostGroupName, final String type) {
            final List<Mount> candidateMounts = new ArrayList<>();
            List<Mount> mountsForHostGroup;
            if (hostGroupName == null) {
                mountsForHostGroup = rewriteMount.getVirtualHost().getVirtualHosts()
                        .getMountsByHostGroup(rewriteMount.getVirtualHost().getHostGroupName());
            } else {
                mountsForHostGroup = rewriteMount.getVirtualHost().getVirtualHosts().getMountsByHostGroup(hostGroupName);
                if(mountsForHostGroup == null || mountsForHostGroup.isEmpty()) {
                    log.debug("Did not find any Mount for hostGroupName '{}'. Return empty list for canonicalLinks.");
                    return Collections.emptyList();
                }
            }

                /*
                 * There can be multiple suited Mount's (for example the Mount for preview and composermode can be the 'same' subsite). We
                 * choose the candicate mounts as follows:
                 *
                 * If the 'type' argument is not null, the candidate mount must at least have one of it types equal to type.
                 * Else : The Mount must have at least ONE type in common as the current Mount of this HstLinkResolver
                 *
                 * The candidate Mount must have a #getCanonicalContentPath() or #getContentPath() that is a prefix of the nodePath
                 *
                 */

            for (Mount candidateMount : mountsForHostGroup) {

                if(!candidateMount.isMapped()) {
                    // not a mount for a HstSite
                    continue;
                }

                // (a)
                if (rewritePath.startsWith(candidateMount.getContentPath() + "/") || rewritePath.equals(candidateMount.getContentPath())) {
                    // check whether one of the types of this Mount matches the types of the currentMount: if so, we have a possible hit.
                    // (b)
                    if (type != null) {
                        if (candidateMount.getTypes().contains(type)) {
                            candidateMounts.add(candidateMount);
                        } else {
                            log.debug("Mount '{}' has the correct canonical content path to link rewrite '{}', but " +
                                            "it does not have at least one type equal to '{}' hence cannot be used. Try next one",
                                    candidateMount, rewritePath, type);
                        }
                    } else if (Collections.disjoint(candidateMount.getTypes(), rewriteMount.getTypes())) {
                        // The Mount did not have a type in common with the current Mount. Try another one.
                        log.debug("Mount '{}' has the correct canonical content path to link rewrite '{}', but it " +
                                        "does not have at least one type in common with the current request Mount '{}'. Try next one",
                                candidateMount, rewritePath, rewriteMount);
                    } else {
                        log.debug("Found a Mount '{}' where the nodePath '{}' belongs to. Add this Mount to the " +
                                "list of possible suited mounts", candidateMount, rewritePath);
                        candidateMounts.add(candidateMount);
                    }
                }
            }
            return candidateMounts;
        }


        private List<HstLinkImpl> resolveToHstLinkList(String nodePath, Mount tryMount, ResolverProperties resolverProperties){
            List<HstLinkImpl> hstLinkList = new ArrayList<>();
            if(!resolverProperties.virtual && nodePath.equals(tryMount.getContentPath())) {
                // the root node of the site. Return the homepage
                String pathInfo = HstSiteMapUtils.getPath(tryMount, tryMount.getHomePage());
                if (pathInfo != null) {
                    hstLinkList.add(new HstLinkImpl(pathInfo, tryMount, false));
                }
            } else if(!resolverProperties.virtual && nodePath.startsWith(tryMount.getContentPath() + "/")) {
                String relPath = nodePath.substring(tryMount.getContentPath().length());
                List<ResolvedLocationMapTreeItem> resolvedLocations = resolveToAllLocationMapTreeItem(relPath, tryMount, resolverProperties);
                for (ResolvedLocationMapTreeItem resolvedLocation : resolvedLocations) {
                    if (resolvedLocation.getPath() != null) {
                        hstLinkList.add(new HstLinkImpl(resolvedLocation, tryMount,false));
                    }
                }
            } else if (resolverProperties.virtual && nodePath.equals(tryMount.getContentPath())) {
                // the root node of the site. Return the homepage
                String pathInfo = HstSiteMapUtils.getPath(tryMount, tryMount.getHomePage());
                if (pathInfo != null) {
                    hstLinkList.add(new HstLinkImpl(pathInfo, tryMount, false));
                }
            }  else if (resolverProperties.virtual && nodePath.startsWith(tryMount.getContentPath()  + "/")) {
                String relPath = nodePath.substring(tryMount.getContentPath().length());
                List<ResolvedLocationMapTreeItem> resolvedLocations = resolveToAllLocationMapTreeItem(relPath, tryMount, resolverProperties);
                for (ResolvedLocationMapTreeItem resolvedLocation : resolvedLocations) {
                    if (resolvedLocation.getPath() != null) {
                        hstLinkList.add(new HstLinkImpl(resolvedLocation, tryMount, false));
                    }
                }
            } else if (isBinaryLocation(nodePath)) {
                log.debug("Binary path, return hstLink prefixing this path with '{}'", BINARIES_PREFIX);
                final HstLink hstLinkForBinaryLocation = createHstLinkForBinaryLocation(nodePath, tryMount);
                if (hstLinkForBinaryLocation != null) {
                    hstLinkList.add((HstLinkImpl)hstLinkForBinaryLocation);
                } else {
                    String pathInfo = BINARIES_PREFIX + nodePath;
                    if (pathInfo != null) {
                        hstLinkList.add(new HstLinkImpl(pathInfo, tryMount, true));
                    }
                }
            }
            return hstLinkList;
        }

        /**
         * Tries to translate the <code>path</code> with the {@link Mount} <code>tryMount</code> to a sitemap pathInfo. If
         * the <code>tryMount<code> does not have a sitemap that is capable of translating the <code>path</code> to a pathInfo, <code>null</code>
         * is returned. If the <code>tryMount</code> is not mapped at all, the <code>path</code> itself is the result
         * @param path
         * @param tryMount
         * @return pathInfo for <code>tryMount</code> or <code>null</code>
         */
        private List<ResolvedLocationMapTreeItem> resolveToAllLocationMapTreeItem(String path, Mount tryMount, ResolverProperties resolverProperties) {
            List<ResolvedLocationMapTreeItem> resolvedLocations = new ArrayList<>();
            if (tryMount.isMapped() && tryMount.getHstSite() != null) {
                LocationMapResolver resolver = new LocationMapResolver(
                                                tryMount.getHstSite().getLocationMapTree(),
                                                tryMount.getHstSite().getLocationMapTreeComponentDocuments());
                resolver.setRepresentsDocument(resolverProperties.representsDocument);
                resolver.setCanonical(resolverProperties.canonicalLink);
                resolver.setResolvedSiteMapItem(resolverProperties.resolvedSiteMapItem);
                resolvedLocations.addAll(resolver.resolveAll(path));
            } else {
                // the Mount does not have a HstSite attached to it. Just use the 'nodePath' we have so far as
                // we do not have a further SiteMap mapping. We only have a site content base path mapping
                resolvedLocations.add(new ResolvedLocationMapTreeItemImpl(path, null, resolverProperties.representsDocument));
            }
            if (log.isDebugEnabled()) {
                if (resolvedLocations.isEmpty()) {
                    log.debug("Could not resolve path '{}' to a sitemap item for mount '{}'. Other mounts will be tried if available.",
                            path,  tryMount);
                } else {
                    for (ResolvedLocationMapTreeItem resolvedLocation : resolvedLocations) {
                        log.debug("Successful resolved path '{}' to '{}' for mount '{}'", path, resolvedLocation, tryMount);
                    }
                }
            }
            return resolvedLocations;
        }


    /**
         * @param nodePath jcr node path
         * @param tryMount the current mount to try
         * @param resolverProperties whether the jcr node path belongs to a virtual node
         * @return LinkInfo for <code>tryMount</code>and <code>nodePath</code> or <code>null</code>
         */
        private HstLink resolveToHstLink(String nodePath, Mount tryMount, ResolverProperties resolverProperties){
            if(!resolverProperties.virtual && nodePath.equals(tryMount.getContentPath())) {
                // the root node of the site. Return the homepage
                String pathInfo = HstSiteMapUtils.getPath(tryMount, tryMount.getHomePage());
                return pathInfo == null ? null : new HstLinkImpl(pathInfo, tryMount, false);
            } else if(!resolverProperties.virtual && nodePath.startsWith(tryMount.getContentPath() + "/")) {
                String relPath = nodePath.substring(tryMount.getContentPath().length());
                ResolvedLocationMapTreeItem resolvedLocation = resolveToLocationMapTreeItem(relPath, tryMount, resolverProperties);
                if (resolvedLocation == null || resolvedLocation.getPath() == null) {
                    return null;
                }
                return new HstLinkImpl(resolvedLocation, tryMount, false);
            } else if (resolverProperties.virtual && nodePath.equals(tryMount.getContentPath())) { 
                // the root node of the site. Return the homepage
                String pathInfo = HstSiteMapUtils.getPath(tryMount, tryMount.getHomePage());
                return pathInfo == null ? null : new HstLinkImpl(pathInfo, tryMount, false);
            }  else if (resolverProperties.virtual && nodePath.startsWith(tryMount.getContentPath()  + "/")) { 
                String relPath = nodePath.substring(tryMount.getContentPath().length());
                ResolvedLocationMapTreeItem resolvedLocation = resolveToLocationMapTreeItem(relPath, tryMount, resolverProperties);
                if (resolvedLocation == null || resolvedLocation.getPath() == null) {
                    return null;
                }
                return new HstLinkImpl(resolvedLocation, tryMount, false);
            } else if (isBinaryLocation(nodePath)) {
                final HstLink hstLinkForBinaryLocation = createHstLinkForBinaryLocation(nodePath, tryMount);
                if (hstLinkForBinaryLocation != null) {
                    return hstLinkForBinaryLocation;
                }
                log.debug("Binary path, return hstLink prefixing this path with '{}'", BINARIES_PREFIX);
                String pathInfo = BINARIES_PREFIX+nodePath;
                return new HstLinkImpl(pathInfo, tryMount, true);
            }
            return null;
        }

        /**
         * Tries to translate the <code>path</code> with the {@link Mount} <code>tryMount</code> to a sitemap pathInfo. If
         * the <code>tryMount<code> does not have a sitemap that is capable of translating the <code>path</code> to a pathInfo, <code>null</code>
         * is returned. If the <code>tryMount</code> is not mapped at all, the <code>path</code> itself is the result
         * @param path
         * @param tryMount
         * @return pathInfo for <code>tryMount</code> or <code>null</code>
         */
        private ResolvedLocationMapTreeItem resolveToLocationMapTreeItem(String path, Mount tryMount, ResolverProperties resolverProperties){
            ResolvedLocationMapTreeItem resolvedLocation = null;
            if (tryMount.isMapped() && tryMount.getHstSite() != null) {
                if (resolverProperties.preferredItem != null) {
                    final LocationMapResolver subResolver;

                    subResolver = getSubLocationMapResolver(
                            resolverProperties.preferredItem,
                            tryMount.getHstSite().getComponentsConfiguration(),
                            tryMount.getContentPath(),
                            tryMount.getContextPath());

                    subResolver.setRepresentsDocument(resolverProperties.representsDocument);
                    subResolver.setResolvedSiteMapItem(resolverProperties.resolvedSiteMapItem);
                    subResolver.setCanonical(resolverProperties.canonicalLink);
                    subResolver.setSubResolver(true);
                    resolvedLocation = subResolver.resolve(path);
                    if ((resolvedLocation == null || resolvedLocation.getPath() == null) && !resolverProperties.fallback) {
                        log.debug("Could not resolve path '{}' for preferredItem '{}' for mount '{}'. Fallback is false. " +
                                        "Other mounts will be tried if available.", path, resolverProperties.preferredItem.getId(), tryMount);
                        return null;
                    }
                }
                if (resolvedLocation == null) {
                    LocationMapResolver resolver = new LocationMapResolver(tryMount.getHstSite().getLocationMapTree(),
                            tryMount.getHstSite().getLocationMapTreeComponentDocuments());
                    resolver.setRepresentsDocument(resolverProperties.representsDocument);
                    resolver.setCanonical(resolverProperties.canonicalLink);
                    resolver.setResolvedSiteMapItem(resolverProperties.resolvedSiteMapItem);
                    resolvedLocation = resolver.resolve(path);
                }
                if (resolvedLocation != null && resolvedLocation.getPath() != null) {
                    log.debug("Successful resolved path '{}' to '{}' for mount '{}'", path, resolvedLocation, tryMount);
                    return resolvedLocation;
                }
                log.debug("Could not resolve path '{}' to a sitemap item for mount '{}'. Other mounts will be tried if available.",
                        path,  tryMount);
                return null;

            } else {
                // the Mount does not have a HstSite attached to it. Just use the 'nodePath' we have so far as
                // we do not have a further SiteMap mapping. We only have a site content base path mapping
                final ResolvedLocationMapTreeItemImpl r = new ResolvedLocationMapTreeItemImpl(path, null, resolverProperties.representsDocument);
                log.debug("Mount '{}' does not have a sitemap. Return unmapped resolved location '{}'", tryMount, r);
                return r;
            }
        }

    }
    
    private static class ResolverProperties {
        
        ResolvedSiteMapItem resolvedSiteMapItem;
        HstSiteMapItem preferredItem;
        boolean virtual;
        /*
         * when allowOtherMounts = true, we try other mounts if the mount from this HstLinkResolver cannot resolve the nodePath
         */
        boolean tryOtherMounts = true;
        boolean canonicalLink;
        boolean representsDocument;
        boolean fallback;
        boolean navigationStateful;
    }



    static class CandidateMountComparator implements Comparator<Mount> {

        Mount referenceMount;
        CandidateMountComparator(Mount referenceMount) {
            this.referenceMount = referenceMount;
        }
        @Override
        public int compare(Mount mount1, Mount mount2) {
            // Algorithm step 1: order the mounts with the same primary type as the referenceMount to be first
            boolean equal1 = mount1.getType().equals(referenceMount.getType());
            boolean equal2 = mount1.getType().equals(referenceMount.getType());
            if(equal1 != equal2) {
                // if equal2 is true and equal1 not, then the order must be flipped (return +1). Otherwise, keep as is and return -1
                if(equal2) {
                    return 1;
                }
                return -1;
            }
            
            // Algorithm step 2: order the mounts that have the most 'types' in common with the referenceMount to be first
            int inCommon1 = countCommon(mount1.getTypes(), referenceMount.getTypes());
            int inCommon2 = countCommon(mount2.getTypes(), referenceMount.getTypes());
            
            if(inCommon1 != inCommon2) {
                // if inCommon2 is larger than inCommon1, then the order must be flipped (return +1). Otherwise, keep as is and return -1
                if(inCommon2 > inCommon1) {
                    return 1;
                }
                return -1;
            }
            
            // Algorithm step 3: order the mounts to have the ones with the fewest types first: 
            int nrTypes1 = mount1.getTypes().size();
            int nrTypes2 = mount2.getTypes().size();
            if(nrTypes1 != nrTypes2) {
                // if nrTypes2 is smaller than nrTypes1, then the order must be flipped (return +1). Otherwise, keep as is and return -1
                if(nrTypes2 < nrTypes1) {
                    return 1;
                }
                return -1;
            }
            
            // Algorithm step 4: order the mounts to have the ones that have the deepest (most slashes) #getCanonicalContentPath() first
            int depth1 = mount1.getContentPath().split("/").length;
            int depth2 = mount2.getContentPath().split("/").length;
            if(depth1 != depth2) {
               // if depth2 > depth1, then the order must be flipped (return +1). Otherwise, keep as is and return -1
               if(depth2 > depth1) {
                   return 1;
               }
               return -1;
            }
            return 0;
        }
       
        
        private int countCommon(List<String> types, List<String> types2) {
            int counter = 0;
            for(String type : types) {
                if(types2.contains(type)) {
                    counter++;
                }
            }
            return counter;
        }
    }

    static class LowestDepthFirstAndThenLexicalComparator implements Comparator<HstLink> {
        @Override
        public int compare(final HstLink link1, final HstLink link2) {
            final String path1 = link1.getPath();
            final String path2 = link2.getPath();

            if (path1 == null) {
                if (path2 == null) {
                    return 0;
                }
                return 1;
            }
            if (path2 == null) {
                return -1;
            }

            int depth1 = path1.split("/").length;
            int depth2 = path2.split("/").length;
            if (depth1 == depth2) {
                return path1.compareTo(path2);
            }
            return depth1 - depth2;
        }
    }


    private LocationMapResolver getSubLocationMapResolver(final HstSiteMapItem preferredItem,
                                                          final HstComponentsConfiguration componentsConfiguration,
                                                          final String mountContentPath,
                                                          final String contextPath) {
        SubLocationMapTreesHolder subLocationMapTreesHolder = loadedSubLocationMapTreesHolder.get(preferredItem);
        if(subLocationMapTreesHolder == null) {
            subLocationMapTreesHolder = new SubLocationMapTreesHolder(
                    new LocationMapTreeSiteMap(preferredItem),
                    new LocationMapTreeComponentDocuments(preferredItem, componentsConfiguration, mountContentPath, contextPath)
            );

            loadedSubLocationMapTreesHolder.put(preferredItem, subLocationMapTreesHolder);
        }
        return new LocationMapResolver(subLocationMapTreesHolder.subLocationMapTreeSiteMap,
                subLocationMapTreesHolder.subLocationMapTreeComponentDocuments);
    }

    private static class SubLocationMapTreesHolder {
        private LocationMapTree subLocationMapTreeSiteMap;
        private LocationMapTree subLocationMapTreeComponentDocuments;

        public SubLocationMapTreesHolder(final LocationMapTreeSiteMap subLocationMapTreeSiteMap,
                                         final LocationMapTreeComponentDocuments subLocationMapTreeComponentDocuments) {

            this.subLocationMapTreeSiteMap = subLocationMapTreeSiteMap;
            this.subLocationMapTreeComponentDocuments = subLocationMapTreeComponentDocuments;
        }
    }

}
