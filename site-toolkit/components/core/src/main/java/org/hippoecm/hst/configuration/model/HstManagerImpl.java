/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.configuration.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.StringPool;
import org.hippoecm.hst.configuration.channel.MutableChannelManager;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerRegistry;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the loading of {@link HstNodeImpl}'s. 
 */
public class HstManagerImpl implements HstManager {
    
    private static final Logger log = LoggerFactory.getLogger(HstManagerImpl.class);

    private Repository repository;
    private Credentials credentials;

    private volatile VirtualHosts virtualHosts;
    private HstURLFactory urlFactory;
    private HstSiteMapMatcher siteMapMatcher;
    private HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;
    
    private HstComponentRegistry componentRegistry;
    private HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry;
    private HstLinkCreator hstLinkCreator;
    
    /**
     * This is a {@link HstComponentsConfigurationService} instance cache : When all the backing HstNode's for 
     * hst:pages, hst:components, hst:catalog and hst:templates, then, the HstComponentsConfiguration object can be shared between different Mounts. 
     * The key is the Set of all HstNode path's directly below the components, pages, catalog and templates : The path uniquely defines the HstNode
     * and there is only inheritance on the nodes directly below components, pages, catalog and templates: Since no fine-grained inheritance, these
     * HstNode's identify uniqueness 
     * Also this cache is reused when a configuration change did not impact HstComponentsConfiguration's at all
     */
    private Map<Set<String>, HstComponentsConfigurationService> hstComponentsConfigurationInstanceCache = new HashMap<Set<String>, HstComponentsConfigurationService>();;
    
    
    /**
     * The root path of all the hst configuations nodes, by default /hst:hst
     */
    private String rootPath;
    
    /**
     * the default cms preview prefix : The prefix all URLs when accessed through the CMS 
     */
    private String cmsPreviewPrefix;
   
    /**
     * The depth, or length of the rootPath splitted on slash. Thus for example /hst:hst returns 2 
     */
    private int rootPathDepth;
   
    /**
     * The root of the virtual hosts node. There should always be exactly one.
     */
    private HstNode virtualHostsNode; 
    
    /**
     * The common catalog node and <code>null</code> if there is no common catalog (hst:configurations/hst:catalog)
     */
    private HstNode commonCatalog;

    /**
     * The map of all configurationRootNodes where the key is the path to the configuration: This is the non enhanced map: in other words,
     * no hstconfiguration inheritance is accounted for. This is the plain hierarchical jcr tree translation to HstNode tree
     */
    private Map<String, HstNode> configurationRootNodes = new HashMap<String, HstNode>();
    
    /**
     * The enhanced version of configurationRootNodes : During enhancing, the inheritance (hst:inheritsfrom) is resolved. Note
     * that the original HstNode's in configurationRootNodes are not changed. Thus, all HstNode's in configurationRootNodes are 
     * first copied to new instances. The backing provider is allowed to be the same instance still.
     */
    private Map<String, HstNode> enhancedConfigurationRootNodes = new HashMap<String, HstNode>();

    /**
     * The map of all site nodes where the key is the path
     */
    private Map<String, HstSiteRootNode> siteRootNodes = new HashMap<String, HstSiteRootNode>();
    
    /**
     * Request path suffix delimiter
     */
    private String pathSuffixDelimiter = "./";
    
    /**
     * The list of implicit configuration augmenters which can provide extra hst configuration after the {@link VirtualHosts} object 
     * has been created
     */
    List<HstConfigurationAugmenter> hstConfigurationAugmenters = new ArrayList<HstConfigurationAugmenter>();
    

    private boolean clearAll = false;
    private Map<HstEvent.ConfigurationType, Set<HstEvent>> configChangeEventMap;
    private MutableChannelManager channelManager;

    public synchronized void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public synchronized void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
    
    public synchronized void setRootPath(String rootPath) {
        this.rootPath = rootPath;
        this.rootPathDepth = rootPath.split("/").length;
    }

    public synchronized String getRootPath() {
        return rootPath;
    }
    
    public void setComponentRegistry(HstComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }
    
    public void setSiteMapItemHandlerRegistry(HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry) {
        this.siteMapItemHandlerRegistry = siteMapItemHandlerRegistry;
    }
    
    public synchronized String getCmsPreviewPrefix() {
        return cmsPreviewPrefix;
    }

    public synchronized void setCmsPreviewPrefix(String cmsPreviewPrefix) {
        this.cmsPreviewPrefix = cmsPreviewPrefix;
    }
    
    public void setUrlFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }

    public HstURLFactory getUrlFactory() {
        return this.urlFactory;
    }
    
    public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher) {
        this.siteMapMatcher = siteMapMatcher;
    }
    
    public HstSiteMapMatcher getSiteMapMatcher() {
        return siteMapMatcher;
    }
    
    @Override
    public List<HstConfigurationAugmenter> getHstConfigurationAugmenters() {
        return hstConfigurationAugmenters;
    }
    
    /**
     * Adds <code>hstConfigurationProvider</code> to {@link #hstConfigurationAugmenters}
     * @param augmenter
     */
    public void addHstConfigurationAugmenter(HstConfigurationAugmenter augmenter) {
        hstConfigurationAugmenters.add(augmenter);
    }
    
    public void setSiteMapItemHandlerFactory(HstSiteMapItemHandlerFactory siteMapItemHandlerFactory) {
        this.siteMapItemHandlerFactory = siteMapItemHandlerFactory;
    }
    
    public HstSiteMapItemHandlerFactory getSiteMapItemHandlerFactory() {
        return siteMapItemHandlerFactory;
    } 
    
    public String getPathSuffixDelimiter() {
        return pathSuffixDelimiter;
    }
    
    public void setPathSuffixDelimiter(String pathSuffixDelimiter) {
        this.pathSuffixDelimiter = pathSuffixDelimiter;
    }

    public void setChannelManager(MutableChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public void setHstLinkCreator(HstLinkCreator hstLinkCreator) {
        this.hstLinkCreator = hstLinkCreator;
    }
    
    public VirtualHosts getVirtualHosts() throws RepositoryNotAvailableException {
 
        VirtualHosts currentHosts = virtualHosts;
        if (currentHosts == null) {
            synchronized(this) {
                if (virtualHosts == null) {
                    buildSites(); 
                    if(virtualHosts == null) {
                        throw new IllegalStateException("The HST configuration model could not be loaded. Cannot process request");
                    }
                }
                currentHosts = virtualHosts;
            }
        }
        
        return currentHosts;
    }

    protected void buildSites() throws RepositoryNotAvailableException {
        log.info("Start building in memory hst configuration model");
        long start = System.currentTimeMillis();
        
        if (clearAll) { 
            configChangeEventMap = null;
            commonCatalog = null;
            configurationRootNodes.clear();
            siteRootNodes.clear();
        }

        Session session = null;

        try {
            if (this.credentials == null) {
                session = this.repository.login();
            } else {
                session = this.repository.login(this.credentials);
            }
            
            // session can come from a pooled event based pool so always refresh before building configuration:
            session.refresh(false);
            
            // get all the root hst virtualhosts node: there is only allowed to be exactly ONE
            {   
                if(virtualHostsNode == null) {
                    Node virtualHostsJcrNode = session.getNode(rootPath + "/hst:hosts");
                    virtualHostsNode = new HstNodeImpl(virtualHostsJcrNode, null, true);
                } else {
                    // do finegrained reloading, removing and loading of previously loaded nodes that changed.
                    

                    Set<String> loadNodes = new HashSet<String>();
                    int pathLengthHstHostNode = (rootPath + "/hst:hosts").length();
                    if(configChangeEventMap != null) {
                        Set<HstEvent> events = configChangeEventMap.get(HstEvent.ConfigurationType.HOST_NODE);
                        
                        for(HstEvent event : events) {
                            if(event.eventType == HstEvent.EventType.NODE_EVENT) { 
                                if(event.jcrEventType == Event.NODE_REMOVED) {
                                    String path = event.path.substring(pathLengthHstHostNode);
                                    if(path.length() == 0) {
                                        // the root has been removed / moved
                                        // we can do no better than set virtualHosts to 'null', remove 
                                        // the events for the HOST_NODE, and try again the next request
                                        virtualHosts = null;
                                        events.clear();
                                        virtualHostsNode = null;
                                        log.warn("The node '{}' has been removed. Cannot reload model.", rootPath + "/hst:hosts");
                                        return;
                                    } 
                                    HstNode node = virtualHostsNode.getNode(path.substring(1));
                                    if(node != null) {
                                        node.getParent().removeNode(node.getValueProvider().getName());
                                    }
                                } else if(event.jcrEventType == Event.NODE_ADDED) {
                                    loadNodes.add(event.path);
                                } else if(event.jcrEventType == Event.NODE_MOVED) {
                                    log.error("NODE MOVE not used because jackrabbit returns a delete and an add instead. This should not be possible");
                                } 
                            } else {
                                // PROPERTY EVENT : we mark the HstNode as stale
                                String path = event.path.substring(pathLengthHstHostNode);
                                HstNode node;
                                if(path.length() == 0) {
                                    node = virtualHostsNode;
                                } else {
                                    path = path.substring(1);
                                    node = virtualHostsNode.getNode(path);
                                }
                                 
                                if(node != null) {
                                    ((HstNodeImpl)node).markStale(); 
                                }
                            }
                        }
                    }
                    
                    if(virtualHostsNode.getNodes().isEmpty()) {
                        // just reload everything
                        Node virtualHostsJcrNode = session.getNode(rootPath + "/hst:hosts");
                        virtualHostsNode = new HstNodeImpl(virtualHostsJcrNode, null, true);
                    } else { 
                       //First load all added nodes. 
                        for(String path : loadNodes) {
                            if(virtualHostsNode.getNode(path.substring(pathLengthHstHostNode +1)) != null) {
                                // already loaded by parent
                                continue;
                            }
                            String parentPath = path.substring(0,path.lastIndexOf("/"));
                            HstNode parentNode;
                            if(parentPath.equals(rootPath + "/hst:hosts")) {
                                parentNode = virtualHostsNode;
                            } else {
                                parentNode = virtualHostsNode.getNode(parentPath.substring(pathLengthHstHostNode +1));
                            }
                            if(parentNode == null) {
                                 // do nothing: node will be loaded by a parent later on
                            } else {
                                // reload now the path and add it to the parent
                                if(session.nodeExists(path)) {
                                    HstNode node = new HstNodeImpl(session.getNode(path), parentNode, true);
                                    parentNode.addNode(node.getValueProvider().getName(), node);
                                }
                            }
                        }
                       // now iterate through the tree, and reload the valueprovider for all HstNode's that are marked stale
                       traverseAndReloadIfNeeded(virtualHostsNode, session);
                    }
                }
            } 
            
            // check whether there is an event that says that the common catalog changed
            if(configChangeEventMap != null && configChangeEventMap.containsKey(HstEvent.ConfigurationType.COMMON_CATALOG_NODE) && !configChangeEventMap.get(HstEvent.ConfigurationType.COMMON_CATALOG_NODE).isEmpty()) {
                commonCatalog = null;
            }
            
            // if there is a common catalog that is not yet loaded, we load this one:
            if(commonCatalog == null && session.itemExists(rootPath +"/hst:configurations/hst:catalog")) {
                // we have a common catalog. Load this catalog. It is available for every (sub)site
                Node catalog = (Node)session.getItem(rootPath +"/hst:configurations/hst:catalog");
                commonCatalog = new HstNodeImpl(catalog, null, true);
            } 
  
            // get all the root hst configuration nodes
            boolean hstComponentsConfigurationChanged = false;
            { 
                if(configurationRootNodes.isEmpty()) {
                    loadAllConfigurationNodes(session);
                    hstComponentsConfigurationChanged = true;
                } else {
                    // do finegrained reloading, removing and loading of previously loaded nodes that changed.

                    Set<String> loadNodes = new HashSet<String>();
                    if(configChangeEventMap != null) {
                        Set<HstEvent> events = configChangeEventMap.get(HstEvent.ConfigurationType.HSTCONFIGURATION_NODE);
                        if(events.size() > 0) {
                            hstComponentsConfigurationChanged = true;
                        }
                        
                        /*
                         * When a node is removed and added, we need to reload the parent because 
                         * the ordering is most likely changed: Jackrabbit returns for a MOVE a 'remove' and an 'add' as event.
                         * we keep track of removals in removedPaths. When we also later encounter an add, we reload the parent
                         */
                        Map<String, HstNode> removedPathsForParentNode = new HashMap<String, HstNode>();
                          
                        for(HstEvent event : events) {
                            if(event.eventType == HstEvent.EventType.NODE_EVENT) { 
                                if(event.jcrEventType == Event.NODE_REMOVED) {
                                    HstNode node = getConfigurationNodeForPath(event.path);
                                    if(node != null) {
                                        if(node.getParent() != null) {
                                            removedPathsForParentNode.put(node.getValueProvider().getPath(), node.getParent());
                                            node.getParent().removeNode(node.getValueProvider().getName());
                                        } else {
                                            // we are a root
                                            configurationRootNodes.remove(event.path);
                                        }
                                    }
                                } else if(event.jcrEventType == Event.NODE_ADDED) {
                                    loadNodes.add(event.path);
                                } else if(event.jcrEventType == Event.NODE_MOVED) {
                                    log.error("NODE MOVE not used because jackrabbit returns a delete and an add instead. This should not be possible");
                                } 
                            } else {
                                // PROPERTY EVENT : we mark the HstNode as stale
                                HstNode node = getConfigurationNodeForPath(event.path);
                                if(node != null) {
                                    ((HstNodeImpl)node).markStale(); 
                                }
                            }
                        } 
                        
                        // check whether there were removes and adds for the same node (in other words, a MOVE)
                        List<String> extraNodesToLoad = new ArrayList<String>();
                        for(String path : loadNodes) { 
                            if(removedPathsForParentNode.containsKey(path)) {
                                // found a move and add. Remove the parent and reload the parent
                                HstNode node = removedPathsForParentNode.get(path);
                                if(node.getParent() != null) {
                                    node.getParent().removeNode(node.getValueProvider().getName());
                                    extraNodesToLoad.add(node.getValueProvider().getPath());
                                } else {
                                    // we are a root
                                    configurationRootNodes.remove(node.getValueProvider().getPath());
                                }
                            }
                        }
                        
                        loadNodes.addAll(extraNodesToLoad);
                    }
                    
                    if( configurationRootNodes.isEmpty()) {
                        loadAllConfigurationNodes(session);
                    } else {
                        // do finegrained loading and reloading. 
                         
                        //First load all added nodes.  
                        for(String path : loadNodes) {
                            if(getConfigurationNodeForPath(path) != null) {
                                // already loaded by parent
                                continue;
                            }
                            String parentPath = path.substring(0,path.lastIndexOf("/"));
                            HstNode parentNode = getConfigurationNodeForPath(parentPath);
                            if(parentNode == null) {
                                // there is no parent. Parent will still be added later so skip for now, OR we are a 
                                // rootConfigurationNode
                                if(path.split("/").length == rootPathDepth + 2) {
                                    // this is a rootConfigurationNode. load it now. It can also already been removed
                                    if(session.nodeExists(path)) {
                                        HstNode hstNode = new HstNodeImpl(session.getNode(path), null, true);
                                        configurationRootNodes.put(hstNode.getValueProvider().getPath(), hstNode);
                                    }
                                } else {
                                    // do nothing: node will be loaded by a parent
                                }
                            } else {
                                // reload now the path and add it to the parent
                                if(session.nodeExists(path)) {
                                    HstNode node = new HstNodeImpl(session.getNode(path), parentNode, true);
                                    parentNode.addNode(node.getValueProvider().getName(), node);
                                }
                            }
                        }
                        // now iterate through the tree, and reload the valueprovider for all HstNode's that are marked stale
                        for(HstNode node : configurationRootNodes.values()) {
                            traverseAndReloadIfNeeded(node, session);
                        }
                    }
                    
                }
            }
            // when there was a change or total reload, empty the cache
            if(hstComponentsConfigurationChanged) {
                hstComponentsConfigurationInstanceCache.clear();
                // since hst config changed, also unregister component registry
                componentRegistry.unregisterAllComponents();
            }
            
            
            // get all the hst:site's 
            if (siteRootNodes.isEmpty()) {
                loadAllSiteNodes(session);
            } else {
                // do finegrained reloading, removing and loading of previously loaded nodes that changed.
                Set<String> loadNodes = new HashSet<String>();
                if(configChangeEventMap != null) {
                    Set<HstEvent> events = configChangeEventMap.get(HstEvent.ConfigurationType.SITE_NODE);
                    for (HstEvent event : events) {
                        if (event.eventType == HstEvent.EventType.NODE_EVENT 
                                || event.eventType == HstEvent.EventType.PROP_EVENT) {
                            if (event.jcrEventType == Event.NODE_REMOVED) {
                                String[] elems = event.path.split("/");
                                // check if it is a node of types hst:sites : in this case, we'll reload all sites
                                if (elems.length == rootPathDepth + 1) {
                                    // change in hst:sites. clear all siteRootNodes
                                    siteRootNodes.clear();
                                    break;
                                } else {
                                    StringBuilder path2Remove = new StringBuilder();
                                    for (int i = 1; i <= rootPathDepth + 1; i++) {
                                        path2Remove.append("/").append(elems[i]);
                                    }
                                    siteRootNodes.remove(path2Remove.toString());
                                }
     
                            } else if (event.jcrEventType == Event.NODE_MOVED) {
                                log.error("NODE MOVE not used because jackrabbit returns a delete and an add instead. This should not be possible");
                            } else {
                                // if a node was not removed, we will reload the hst:site, also for property changes
                                String[] elems = event.path.split("/");
                                if (elems.length == rootPathDepth + 1) {
                                    // change in hst:sites. clear all siteRootNodes
                                    siteRootNodes.clear();
                                    break;
                                }
                                StringBuilder pathLoad = new StringBuilder();
                                for (int i = 1; i <= rootPathDepth + 1; i++) {
                                    pathLoad.append("/").append(elems[i]);
                                }
                                loadNodes.add(pathLoad.toString());
    
                            }
                        }
                    }
                }

                if (siteRootNodes.isEmpty()) {
                    loadAllSiteNodes(session);
                } else {
                    // Reload all added or changed nodes.  
                    for (String path : loadNodes) {
                        if (path.split("/").length == rootPathDepth + 2) {
                            // this is a rootConfigurationNode. load it now. It can also already been removed
                            if (session.nodeExists(path)) {
                                Node rootSiteNode = session.getNode(path);
                                if(rootSiteNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITE)) {
                                    HstSiteRootNode hstRootSiteNode = new HstSiteRootNodeImpl(rootSiteNode, null);
                                    siteRootNodes.put(hstRootSiteNode.getValueProvider().getPath(), hstRootSiteNode);
                                } else {
                                    log.error("We can only load nodes of site '{}' here. This should not be happening.", HstNodeTypes.NODETYPE_HST_SITE);
                                }
                            }
                        } else {
                            log.error("It is not possible to load '{}' because is not a site root node", path);
                        }
                    }
                }
            }

            try {
                // unregister all existing siteMapItemHandlers first
                siteMapItemHandlerRegistry.unregisterAllSiteMapItemHandlers();
                enhancedConfigurationRootNodes = enhanceHstConfigurationNodes(configurationRootNodes);
                this.virtualHosts = new VirtualHostsService(virtualHostsNode, this);

                for(HstConfigurationAugmenter configurationAugmenter : hstConfigurationAugmenters ) {
                    configurationAugmenter.augment(this);
                }
                log.info("Finished build in memory hst configuration model in " + (System.currentTimeMillis() - start) + " ms.");
            } catch (ServiceException e) {
                throw new RepositoryNotAvailableException(e);
            }

            this.channelManager.load(virtualHosts, session);

        } catch (PathNotFoundException e) {
            throw new IllegalStateException("Exception during loading configuration nodes. The HST model cannot be loaded. ",e);
        } catch (RepositoryException e) {
            throw new RepositoryNotAvailableException("Exception during loading configuration nodes. ",e);
        } finally {
            // clear the StringPool as it is not needed any more
            StringPool.clear();
            enhancedConfigurationRootNodes.clear();
            configChangeEventMap = null;
            hstLinkCreator.clear();
            clearAll = false;

            if (session != null) {
                try { 
                    session.logout(); 
                } catch (Exception ce) {
                    throw new RepositoryNotAvailableException("Exception while loging out jcr session ",ce);
                }
            }
        }

    } 
       
    private Map<String, HstNode> enhanceHstConfigurationNodes(Map<String, HstNode> nodes) {
        Map<String, HstNode> enhanced = new HashMap<String, HstNode>();
        for(HstNode node : nodes.values()) {
            HstNode enhancedNode = new HstSiteConfigurationRootNodeImpl((HstNodeImpl)node, nodes, rootPath);
            enhanced.put(enhancedNode.getValueProvider().getPath(), enhancedNode);
        }
        return enhanced;
    }
 
    private void traverseAndReloadIfNeeded(HstNode node, Session session) throws RepositoryException {
       if(node.isStale()) {
           if(session.nodeExists(node.getValueProvider().getPath())) {
               JCRValueProvider provider = new JCRValueProviderImpl(session.getNode(node.getValueProvider().getPath()), false);
               node.setJCRValueProvider(provider);
           } else {
               log.error("Unable to reload an HstNode as it has been deleted. Should not be possible at this place");
           }
       } 
       for(HstNode child : node.getNodes()) {
           traverseAndReloadIfNeeded(child, session);
       }
    }

    /**
     * returns the HstNode for hst configuration and null when not found
     * @param path
     * @return
     */
    private HstNode getConfigurationNodeForPath(String path) {
        String[] elems = path.split("/");
        if(elems.length < rootPathDepth + 2) {
            // cannot be a config node
            return null;
        } else if(elems.length == rootPathDepth + 2) {
            // this is a root locations, namely at rootPath + "/hst:configurations/myproj"
            return configurationRootNodes.get(path);
        } else {
            // try to first find a configuration rootNode, and then the correct child node
            // elems[0] is empty part because path starts with a '/'
            
            StringBuilder rootConfigNodePath = new StringBuilder();
            for(int i = 1; i <= rootPathDepth + 1; i++) {
                rootConfigNodePath.append("/").append(elems[i]);
            }
            HstNode node = configurationRootNodes.get(rootConfigNodePath.toString());
            if(node == null) {
                return null;
            }
            // relPath is the path after the rootConfigNodePath
            String relPath = path.substring(rootConfigNodePath.toString().length() + 1);
            return node.getNode(relPath);
        }
        
    }
    
   
    private void loadAllConfigurationNodes(Session session) throws PathNotFoundException, RepositoryException {
        Node configurationsNode = session.getNode(rootPath + "/hst:configurations");
        NodeIterator configurationRootJcrNodes = configurationsNode.getNodes();
        while(configurationRootJcrNodes.hasNext()) {
            Node configurationRootNode = configurationRootJcrNodes.nextNode();
            if(configurationRootNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
                try {
                    HstNode hstNode = new HstNodeImpl(configurationRootNode, null, true);
                    configurationRootNodes.put(hstNode.getValueProvider().getPath(), hstNode);
                } catch (HstNodeException e) {
                    log.error("Exception while creating Hst configuration for '"+configurationRootNode.getPath()+"'. Fix configuration" ,e);
                } 
            }
        }
    }
    
    private void loadAllSiteNodes(Session session) throws RepositoryException, PathNotFoundException {
        NodeIterator nodes = session.getNode(rootPath).getNodes();
        while(nodes.hasNext()) {
            Node node = nodes.nextNode();
            if(node.isNodeType(HstNodeTypes.NODETYPE_HST_SITES)) {
                NodeIterator siteRootJcrNodes = node.getNodes();
                while(siteRootJcrNodes.hasNext()) {
                    Node rootSiteNode = siteRootJcrNodes.nextNode();
                    HstSiteRootNode hstSiteRootNode = new HstSiteRootNodeImpl(rootSiteNode, null);
                    siteRootNodes.put(hstSiteRootNode.getValueProvider().getPath(), hstSiteRootNode);
                }
            }
        }
    }

    @Override
    public void invalidate(EventIterator events) {
         
        synchronized(this) {
            /* 
             * below, we are going to prepare which HstNode's should be reloaded in our model. 
             * Depending on the change in the jcr node in the hst configuration we will mark either:
             * below we first collide all events for the same nodes to one eventPath
             * all nodes that have been added or moved or deleted or have a property added/changed/removed
             */

            Map<String, HstEvent> nodeIdentifierToEventMap = new HashMap<String, HstEvent>();
            
            try {
                while (events.hasNext()) {
                    Event ev = events.nextEvent();
                    switch (ev.getType()) {
                        // for node events, we always add (even if exists, we then replace) to the map. For property changes, we only add to the 
                        // nodeIdentifierToEventMap when not already present
                        case Event.NODE_ADDED:
                            nodeIdentifierToEventMap.put(ev.getIdentifier(), new HstEvent(ev.getPath(), HstEvent.EventType.NODE_EVENT, ev.getType()));
                            break;
                        case Event.NODE_REMOVED:

                            /*
                             * because jackrabbit returns a remove and an add for a move in the console, a removed and add
                             * has the same identifier. Hence, we add a indication to the identifier
                             */
                            nodeIdentifierToEventMap.put(ev.getIdentifier() + "-removed", new HstEvent(ev.getPath(), HstEvent.EventType.NODE_EVENT, ev.getType()));
                            break;
                        case Event.NODE_MOVED:
                            nodeIdentifierToEventMap.put(ev.getIdentifier(), new HstEvent(ev.getPath(), HstEvent.EventType.NODE_EVENT, ev.getType()));
                            break;
                        case Event.PROPERTY_ADDED:
                            addNodePathIfAbsentForPropertyToMap(nodeIdentifierToEventMap, ev);
                            break;
                        case Event.PROPERTY_CHANGED:
                            addNodePathIfAbsentForPropertyToMap(nodeIdentifierToEventMap, ev);
                            break;
                        case Event.PROPERTY_REMOVED:
                            addNodePathIfAbsentForPropertyToMap(nodeIdentifierToEventMap, ev);
                            break;
                      }
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException happened. Invalidate hst model completely", e);
                invalidateVirtualHosts();
                clearAll = true;
                return;
            }
            
            // now that we have the changed and deleted maps, let's compute which hst:configuration nodes need to be reloaded,
            // which hst:sites and which hst:hosts

            // NOTE we cannot directly know by only the path whether it is a change in a hst:sites or hst:site node: this
            // is because the cnd allowed the hst:sites to have any name: Currently the cnd is:
            // [hst:hst] > nt:base, mix:referenceable, mix:versionable
            // + hst:configurations (hst:configurations) = hst:configurations version
            // + hst:hosts (hst:virtualhosts) = hst:virtualhosts version
            // + hst:blueprints (hst:blueprints) = hst:blueprints version
            // + hst:channels (hst:channels) = hst:channels version
            // + * (hst:sites) = hst:sites version
            
            String hstConfigPath = rootPath+"/hst:configurations";
            String hstCommonCatalogPath = hstConfigPath+"/hst:catalog";
            String hstHostsPath = rootPath+"/hst:hosts";
            String hstBlueprintsPath = rootPath+"/hst:blueprints";
            String hstChannelsPath = rootPath+"/hst:channels";
            
            if(configChangeEventMap == null) {
                configChangeEventMap = new HashMap<HstEvent.ConfigurationType, Set<HstEvent>>(); 
                configChangeEventMap.put(HstEvent.ConfigurationType.HSTCONFIGURATION_NODE, new HashSet<HstEvent>());
                configChangeEventMap.put(HstEvent.ConfigurationType.COMMON_CATALOG_NODE, new HashSet<HstEvent>());
                configChangeEventMap.put(HstEvent.ConfigurationType.HOST_NODE, new HashSet<HstEvent>());
                configChangeEventMap.put(HstEvent.ConfigurationType.SITE_NODE, new HashSet<HstEvent>());
            }
            
            for(HstEvent event : nodeIdentifierToEventMap.values()) {
                if(event.path.startsWith(hstConfigPath+"/") || event.path.equals(hstConfigPath)) {
                    if(event.path.startsWith(hstConfigPath+"/")) {
                        configChangeEventMap.get(HstEvent.ConfigurationType.HSTCONFIGURATION_NODE).add(event);
                    } else {
                        // ignore exact hstConfigPath
                    }
                } else if(event.path.startsWith(hstCommonCatalogPath+"/") || event.path.equals(hstCommonCatalogPath)) {
                    configChangeEventMap.get(HstEvent.ConfigurationType.COMMON_CATALOG_NODE).add(event);
                } else if (event.path.startsWith(hstHostsPath+"/") ||  event.path.equals(hstHostsPath)) {
                    configChangeEventMap.get(HstEvent.ConfigurationType.HOST_NODE).add(event);
                } else if (event.path.startsWith(hstBlueprintsPath+"/") || event.path.equals(hstBlueprintsPath)) {
                    // do nothing for now: TODO
                } else if (event.path.startsWith(hstChannelsPath+"/") || event.path.equals(hstChannelsPath)) {
                    // do nothing for now: TODO
                }
                else {
                    // it must be a change in a hst:sites, a hst:site, or a descendant node
                    configChangeEventMap.get(HstEvent.ConfigurationType.SITE_NODE).add(event);
                } 
            }
            invalidateVirtualHosts();
        }
    }
    
    private void addNodePathIfAbsentForPropertyToMap(Map<String, HstEvent> idPathMapOfChangedAddedOrMovedNodes, Event ev) throws RepositoryException {
        if(idPathMapOfChangedAddedOrMovedNodes.containsKey(ev.getIdentifier())) {
            return;
        } else {
            String propertyPath = ev.getPath();
            String nodePath = propertyPath.substring(0,propertyPath.lastIndexOf("/"));
            idPathMapOfChangedAddedOrMovedNodes.put(ev.getIdentifier(), new HstEvent(nodePath, HstEvent.EventType.PROP_EVENT, ev.getType()));
        }
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
            invalidateVirtualHosts();
            clearAll = true;
        }
    }

    private final void invalidateVirtualHosts() {
        log.info("In memory hst configuration model is invalidated. It will be reloaded on the next request.");
        virtualHosts = null;
        if (channelManager != null) {
            channelManager.invalidate();
        }
    }

    public Map<String, HstSiteRootNode> getHstSiteRootNodes(){
        return siteRootNodes;
    }

    public Map<String, HstNode> getEnhancedConfigurationRootNodes() {
        return enhancedConfigurationRootNodes;
    }
    
    public HstNode getCommonCatalog(){
        return commonCatalog;
    }
    
    /**
     * @return the hstComponentsConfigurationInstanceCache. This {@link Map} is never <code>null</code> 
     */
    public Map<Set<String>, HstComponentsConfigurationService> getHstComponentsConfigurationInstanceCache() {
        return hstComponentsConfigurationInstanceCache;
    }

    static class HstEvent {
        enum EventType {
            NODE_EVENT,
            PROP_EVENT
        }
        
        enum ConfigurationType {
            HOST_NODE,
            SITE_NODE,
            HSTCONFIGURATION_NODE, 
            COMMON_CATALOG_NODE
        }
        
        String path;
        EventType eventType;
        int jcrEventType;
        
        HstEvent(String path , EventType eventType, int jcrEventType) {
            this.path = path;
            this.eventType = eventType;
            this.jcrEventType = jcrEventType;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof HstEvent) {
                HstEvent compare = (HstEvent)obj;
                if(path == null) {
                    return false;
                }
                return path.equals(compare.path) && jcrEventType == compare.jcrEventType;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            if(path != null) {
                return path.hashCode() + jcrEventType;
            }
            return super.hashCode();
        }
        
    }

}
