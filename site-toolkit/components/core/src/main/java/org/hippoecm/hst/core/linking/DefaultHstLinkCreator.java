/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
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

public class DefaultHstLinkCreator implements HstLinkCreator {

    private static final Logger log = LoggerFactory.getLogger(DefaultHstLinkCreator.class);

    private final static String DEFAULT_PAGE_NOT_FOUND_PATH = "pagenotfound";
    private String[] binaryLocations;
    private String binariesPrefix;
    private String pageNotFoundPath = DEFAULT_PAGE_NOT_FOUND_PATH;
    private WeakIdentityMap<HstSiteMapItem, LocationMapTree> loadedSubLocationMapTree = WeakIdentityMap.newConcurrentHashMap();
    private HstLinkProcessor linkProcessor;
    
    private List<LocationResolver> locationResolvers;

    public void setBinariesPrefix(String binariesPrefix){
        this.binariesPrefix = binariesPrefix;
    }
    
    public void setBinaryLocations(String[] binaryLocations) {
        if (binaryLocations == null) {
            this.binaryLocations = null;
        } else {
            this.binaryLocations = new String[binaryLocations.length];
            System.arraycopy(binaryLocations, 0, this.binaryLocations, 0, binaryLocations.length);
        }
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

    
    public HstLink create(Node node, Mount mount) {
        HstLinkResolver linkResolver = new HstLinkResolver(node, mount);
        linkResolver.resolverProperties.tryOtherMounts = false;
        // when linking to a mount, we always want get a canonical link:
        linkResolver.resolverProperties.canonicalLink = true;
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
    public HstLink create(String path, Mount mount) {
        return postProcess(new HstLinkImpl(PathUtils.normalizePath(path), mount));
    }
    
    public HstLink create(String path, Mount mount, boolean containerResource) {
        return postProcess(new HstLinkImpl(PathUtils.normalizePath(path), mount, containerResource));
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
        LocationMapTree subLocationMapTree = loadedSubLocationMapTree.get(preferredItem);
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
       
        Mount mount;
        ResolverProperties resolverProperties;
        boolean isCmsRequest;
      
        /**
         * Create a HstLinkResolver instance with the current <code>requestContext</code>. The {@link Mount} is taken from this context. If
         * we have a {@link ResolvedSiteMapItem} on the <code>requestContext</code>, we also set this also for the {@link HstLinkResolver} for context aware link rewriting
         * @param requestContext
         * @param node
         */
        HstLinkResolver(Node node, HstRequestContext requestContext){
            this.node = node;
            // note: the resolvedSiteMapItem can be null
            resolverProperties = new ResolverProperties();
            resolverProperties.resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
            mount = requestContext.getResolvedMount().getMount();
            isCmsRequest = requestContext.isCmsRequest();
        }
        
        
        /**
         * Create a HstLinkResolver instance for creating a link in this {@link Mount}. We do not take into account the current context from {@link ResolvedSiteMapItem}
         * when creating a {@link HstLinkResolver} through this constructor
         * @param node
         * @param mount
         */
        HstLinkResolver(Node node, Mount mount){
            this.node = node;
            this.mount = mount;
            resolverProperties = new ResolverProperties();
        }
        
        HstLink resolve(){
            if(mount == null) {
                log.info("Cannot create link when the mount is null. Return null");
                return null;
            }
            if(node == null) {
                log.info("Cannot create link when the jcr node null. Return a page not found link");
                return createPageNotFoundLink(mount);
            }
            
            boolean postProcess = true;
            Node canonicalNode = null;
            if(!resolverProperties.navigationStateful) {
                // not context relative, so we try to compute a link wrt the canonical location of the jcr node. If the canonical location is null (virtual only nodes)
                // we'll continue with the non canonical node
                canonicalNode = NodeUtils.getCanonicalNode(node);
            }
            
            LinkInfo linkInfo = null;
            try {
                if(node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                    /*
                     * A hippo resource is not needed to be translated through the HstSiteMap but we create a binary link directly
                     */
                    for(LocationResolver resolver : DefaultHstLinkCreator.this.locationResolvers) {
                        if(node.isNodeType(resolver.getNodeType())) {
                            HstLink link = resolver.resolve(node, mount, mount.getHstSite().getLocationMapTree());
                            if(link != null) {
                               return link; 
                            } else {
                                log.debug("Location resolved for nodetype '{}' is not able to create link for node '{}'. Try next location resolver", resolver.getNodeType(), node.getPath());
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
                    nodePath = node.getPath();
                    if (node.isNodeType(HippoNodeType.NT_FACETSELECT) || node.isNodeType(HippoNodeType.NT_MIRROR)) {
                        node = NodeUtils.getDeref(node);
                        if (node == null) {
                            log.info("Broken content internal link for '{}'. Cannot create a HstLink for it. Return null", nodePath);
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
                    
                    nodePath = node.getPath();
                    
                    linkInfo = resolveToLinkInfo(nodePath, mount, resolverProperties);
                    
                    
                    if (linkInfo == null && resolverProperties.tryOtherMounts) {    
                        
                        log.debug("We cannot create a link for '{}' for the mount '{}' belonging to the current request. Try to create a cross-domain/site/channel link.", nodePath, mount.getName());
                        
                        // when trying other mounts, we do not support 'preferredItem'. Set to null if it wasn't already set to null

                        if(resolverProperties.preferredItem != null) {
                            // cannot use preferredItem and cross domain linking at same time. We set it to null
                            resolverProperties.preferredItem = null;
                            log.info("Trying other mount than current context mount for nodePath '{}'. Cross domain linking cannot be combined with linking to a preferred sitemap item. We'll ignore the preferred item," , nodePath);
                        }
                        
                        /*
                         * The Mount with which this HstLinkResolver was created can not be used to create a link for the nodePath because the path
                         * is out of the scope of the (sub)site or the sitemap of the (sub)site was unable to create a link for it. We'll now try to find a 
                         * Mount that can create a link for it. If there is no Mount that can create a link, a pagenotfound link is created with the original mount.
                         * 
                         * Note that we only create a cross-domain link if and only if there is a Mount that 
                         * 1) Has a #getCanonicalContentPath() or #getContentPath() (for virtual nodes) that start
                         * with or are equal to the 'nodePath' we have here
                         * 2) Belong to the same HostNameGroup as the Mount for this HstLinkResolver (normally the same as for the current request)
                         * 3) Has at least one type (preview, live, composer, etc ) in common with the Mount for this HstLinkResolver 
                         *
                         * Note that if there is a preferredItem we ignore this one for cross domain linking as preferredItem only work within the same Mount
                         */
                        
                        List<Mount> mountsForHostGroup = mount.getVirtualHost().getVirtualHosts().getMountsByHostGroup(mount.getVirtualHost().getHostGroupName());

                        /*
                         * There can be multiple suited Mount's (for example the Mount for preview and composermode can be the 'same' subsite). We
                         * choose the best suited Mount as follows:
                         * a) The Mount must have a #getCanonicalContentPath() or #getContentPath() that is a prefix of the nodePath
                         * b) The Mount must have at least ONE type in common as the current Mount of this HstLinkResolver
                         * 
                         * The resulting list of candidate Mounts can all be tried to create a link for until there is a Mount that
                         * returned a non null LinkInfo. The order in which we try the candidate mount for a LinkInfo is as follows:
                         * 
                         * 1) Firstly order the candidate mounts to have the same primary type as the current Mount of this HstLinkResolver
                         * 2) Secondly order the candidate mounts that have the most 'types' in common with the current Mount of this HstLinkResolver
                         * 3) Thirdly order the Mounts to have the fewest types first: The fewer types it has, and the number of matching types is equal to the current Mount, indicates
                         * that it can be considered more precise
                         * 4) Fourthly order the Mounts first that have the deepest (most slashes) #getCanonicalContentPath() : The deeper the more specific.
                         */
                        List<Mount> candidateMounts = new ArrayList<Mount>();
                        
                        for(Mount candidateMount : mountsForHostGroup) {
                           if(candidateMount == mount) {
                               // do not try the already used mount above again
                               continue;
                           }
                           if(!candidateMount.isMapped()) {
                               // not a mount for a HstSite
                               continue;
                           }
                         
                           // (a)
                           if(nodePath.startsWith(candidateMount.getContentPath() + "/") || nodePath.equals(candidateMount.getContentPath())) {
                              // check whether one of the types of this Mount matches the types of the currentMount: if so, we have a possible hit.
                              // (b)
                               if (Collections.disjoint(candidateMount.getTypes(), mount.getTypes())) {
                                   // The Mount did not have a type in common with the current Mount. Try another one.
                                   log.debug("Mount  ('name = {} and alias = {}') has the correct canonical content path to link rewrite '{}', but it " +
                                           "does not have at least one type in common with the current request Mount hence cannot be used. Try next one",
                                           new String[]{candidateMount.getName(), candidateMount.getAlias(), nodePath});
                               } else {
                                   log.debug("Found a Mount ('name = {} and alias = {}') where the nodePath '{}' belongs to. Add this " +
                                           "Mount to the list of possible suited mounts",
                                           new String[]{candidateMount.getName(), candidateMount.getAlias(), nodePath});
                                   candidateMounts.add(candidateMount);
                               }
                           }
                        }
                        
                        if(candidateMounts.size() == 0) {
                            log.info("There is no Mount available that is suited to linkrewrite '{}'. Return page not found link.", nodePath);
                            return createPageNotFoundLink(mount);
                        } else if(candidateMounts.size() == 1) {
                            linkInfo = resolveToLinkInfo(nodePath, candidateMounts.get(0), resolverProperties);
                        } else {
                            // sort the candidate mounts according the algorithm mount ordering (1), (2), (3) and (4) applied
                            // this is done by the CandidateMountComparator which gets the current 'mount' as reference for
                            // the ordering 
                            
                            Collections.sort(candidateMounts, new CandidateMountComparator(mount));
                            
                            for(Mount tryMount : candidateMounts) {
                                linkInfo = resolveToLinkInfo(nodePath, tryMount, resolverProperties);
                                if(linkInfo != null) {
                                    // succeeded
                                    break;
                                }
                            }
                        }
                        
                      
                    }
                }
            
            } catch(RepositoryException e){
                log.error("Repository Exception during creating link", e);
            }
            
            if(linkInfo == null) {
                log.info("Cannot create a link for node with path '{}'. Return a page not found link", nodePath);
                return createPageNotFoundLink(mount);
            }
            
            HstLink link = new HstLinkImpl(linkInfo.pathInfo, linkInfo.mount, linkInfo.siteMapItem, linkInfo.containerResource);
            if(postProcess) {
                link = postProcess(link);
            }
            return link;
            
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

            if (type == null) {
                type = mount.getType();
            }
            
            boolean postProcess = true;
            Node canonicalNode;
            canonicalNode = NodeUtils.getCanonicalNode(node);
           
            List<LinkInfo> linkInfoList = new ArrayList<LinkInfo>();
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
                
                nodePath = node.getPath(); 
                if(node.isNodeType(HippoNodeType.NT_FACETSELECT) || node.isNodeType(HippoNodeType.NT_MIRROR)) {
                    node = NodeUtils.getDeref(node);
                    if( node == null ) {
                        log.debug("Broken content internal link for '{}'. Cannot create a HstLink for it. Return an empty list for canonical links.", nodePath);
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
                
                nodePath = node.getPath();
                
                // try to get the list of candidateMounts to get a HstLink for

                List<Mount> mountsForHostGroup;
                
                if(hostGroupName == null) {
                    mountsForHostGroup = mount.getVirtualHost().getVirtualHosts().getMountsByHostGroup(mount.getVirtualHost().getHostGroupName());
                } else {
                    mountsForHostGroup = mount.getVirtualHost().getVirtualHosts().getMountsByHostGroup(hostGroupName);    
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

                List<Mount> candidateMounts = new ArrayList<Mount>();
                for(Mount candidateMount : mountsForHostGroup) {
                    
                   if(!candidateMount.isMapped()) {
                       // not a mount for a HstSite
                       continue;
                   }
                 
                   // (a)
                   if(nodePath.startsWith(candidateMount.getContentPath() + "/") || nodePath.equals(candidateMount.getContentPath())) {
                      // check whether one of the types of this Mount matches the types of the currentMount: if so, we have a possible hit.
                      // (b)
                      if(type != null) {
                          if(candidateMount.getTypes().contains(type)) {
                              candidateMounts.add(candidateMount);
                          } else {
                              log.debug("Mount  ('name = {} and alias = {}') has the correct canonical content path to linkrewrite '{}', but " +
                                      "it does not have at least one type equal to '"+type+"' hence cannot be used. Try next one",
                                      new String[]{candidateMount.getName(), candidateMount.getAlias(), nodePath});
                          }
                      } else if (Collections.disjoint(candidateMount.getTypes(), mount.getTypes())) {
                          // The Mount did not have a type in common with the current Mount. Try another one.
                          log.debug("Mount  ('name = {} and alias = {}') has the correct canonical content path to linkrewrite '{}', but it " +
                                  "does not have at least one type in common with the current request Mount hence cannot be used. Try next one",
                                  new String[]{candidateMount.getName(), candidateMount.getAlias(), nodePath});
                      } else {
                          log.debug("Found a Mount ('name = {} and alias = {}') where the nodePath '{}' belongs to. Add this Mount to the " +
                                  "list of possible suited mounts",
                                  new String[]{candidateMount.getName(), candidateMount.getAlias(), nodePath});
                          candidateMounts.add(candidateMount);
                      }
                   }
                }
                
                if(candidateMounts.size() == 0) {
                    log.info("There is no Mount available that is suited to linkrewrite '{}'. Return empty list for canonicalLinks..", nodePath);
                    return Collections.emptyList();
                    
                } 
                      
                for(Mount tryMount : candidateMounts) {
                    LinkInfo linkInfo = resolveToLinkInfo(nodePath, tryMount, resolverProperties);
                    if(linkInfo != null) {
                        linkInfoList.add(linkInfo);
                    }
                }
            
                
            } catch(RepositoryException e){
                log.warn("Repository Exception during creating link", e);
            }
            
            if(linkInfoList.isEmpty()) {
                log.debug("Cannot create any link for node with path '{}'. Return empty list for canonicalLinks.", nodePath);
                return Collections.emptyList();
            }
            
            List<HstLink>  allLinks = new ArrayList<HstLink>();
            for(LinkInfo info : linkInfoList) {

                HstLink link = new HstLinkImpl(info.pathInfo, info.mount, info.siteMapItem, info.containerResource);
                if(postProcess) {
                    link = postProcess(link);
                }
                allLinks.add(link);
            }
            
            return allLinks;
        }

        /**
         * @param nodePath jcr node path
         * @param tryMount the current mount to try 
         * @param resolverProperties whether the jcr node path belongs to a virtual node
         * @return LinkInfo for <code>tryMount</code>and <code>nodePath</code> or <code>null</code>
         */
        private LinkInfo resolveToLinkInfo(String nodePath, Mount tryMount, ResolverProperties resolverProperties){
            if(!resolverProperties.virtual && nodePath.equals(tryMount.getContentPath())) {
                // the root node of the site. Return the homepage
                String pathInfo = HstSiteMapUtils.getPath(tryMount, tryMount.getHomePage());
                return pathInfo == null ? null : new LinkInfo(pathInfo, false, tryMount);
            }
            if(!resolverProperties.virtual && nodePath.startsWith(tryMount.getContentPath() + "/")) {
                String relPath = nodePath.substring(tryMount.getContentPath().length());
                ResolvedLocationMapTreeItem resolvedLocation = resolveToLocationMapTreeItem(relPath, tryMount, resolverProperties);
                return (resolvedLocation == null || resolvedLocation.getPath() == null) ? null : new LinkInfo(resolvedLocation, false, tryMount);
            } else if (resolverProperties.virtual && nodePath.equals(tryMount.getContentPath())) { 
                // the root node of the site. Return the homepage
                String pathInfo = HstSiteMapUtils.getPath(tryMount, tryMount.getHomePage());
                return pathInfo == null ? null : new LinkInfo(pathInfo, false, tryMount);
            }  else if (resolverProperties.virtual && nodePath.startsWith(tryMount.getContentPath()  + "/")) { 
                String relPath = nodePath.substring(tryMount.getContentPath().length());
                ResolvedLocationMapTreeItem resolvedLocation = resolveToLocationMapTreeItem(relPath, tryMount, resolverProperties);
                return (resolvedLocation == null || resolvedLocation.getPath() == null) ? null : new LinkInfo(resolvedLocation, false, tryMount);
            } else if (isBinaryLocation(nodePath)) {
                log.debug("Binary path, return hstLink prefixing this path with '{}'", DefaultHstLinkCreator.this.getBinariesPrefix());
                // Do not postProcess binary locations, as the BinariesServlet is not aware about preprocessing links
                String pathInfo = DefaultHstLinkCreator.this.getBinariesPrefix()+nodePath;
                return pathInfo == null ? null : new LinkInfo(pathInfo, true, tryMount);
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
                    LocationMapResolver subResolver = getSubLocationMapResolver(resolverProperties.preferredItem);
                    subResolver.setRepresentsDocument(resolverProperties.representsDocument);
                    subResolver.setResolvedSiteMapItem(resolverProperties.resolvedSiteMapItem);
                    subResolver.setCanonical(resolverProperties.canonicalLink);
                    subResolver.setSubResolver(true);
                    resolvedLocation = subResolver.resolve(path);
                    if ((resolvedLocation == null || resolvedLocation.getPath() == null) && !resolverProperties.fallback) {
                        log.debug("Could not create a link for '"+path+"' preferredItem '{}' for mount '{}' (host = "+tryMount.getVirtualHost().getHostName()+"). Fallback is false. Other mounts will be tried if available.",resolverProperties.preferredItem.getId(), tryMount.getMountPath());
                        return null;
                    }
                }
                if (resolvedLocation == null) {
                    LocationMapResolver resolver = new LocationMapResolver(tryMount.getHstSite().getLocationMapTree());
                    resolver.setRepresentsDocument(resolverProperties.representsDocument);
                    resolver.setCanonical(resolverProperties.canonicalLink);
                    resolver.setResolvedSiteMapItem(resolverProperties.resolvedSiteMapItem);
                    resolvedLocation = resolver.resolve(path);
                }
                if (resolvedLocation != null && resolvedLocation.getPath() != null) {
                    log.debug("Creating a link for node '{}' succeeded", path);

                    log.info("Succesfull linkcreation for path '{}' to new pathInfo '{}'", path, resolvedLocation.getPath());

                    return resolvedLocation;
                }
                log.debug("Could not create a link for '"+path+"' for mount '{}' (host = {}). Other mounts will be tried if available.", tryMount.getMountPath(), tryMount.getVirtualHost().getHostName());
                return null;

            } else {
                // the Mount does not have a HstSite attached to it. Just use the 'nodePath' we have so far as
                // we do not have a further SiteMap mapping. We only have a site content base path mapping
                return new ResolvedLocationMapTreeItemImpl(path, null);
            }
        }

    }
    
    private class ResolverProperties {
        
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
    
    private class LinkInfo {
        private final String pathInfo;
        private final HstSiteMapItem siteMapItem;
        private final boolean containerResource;
        private final Mount mount;

        private LinkInfo(final String pathInfo, final boolean containerResource, final Mount mount) {
            this.pathInfo = pathInfo;
            this.containerResource = containerResource;
            this.mount = mount;
            this.siteMapItem = null;
        }
        private LinkInfo(final ResolvedLocationMapTreeItem resolvedLocationMapTreeItem, final boolean containerResource, final Mount mount) {
            pathInfo = resolvedLocationMapTreeItem.getPath();
            siteMapItem = resolvedLocationMapTreeItem.getSiteMapItem();
            this.containerResource = containerResource;
            this.mount = mount;
        }
    }
    
    private class CandidateMountComparator implements Comparator<Mount> {

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

}
