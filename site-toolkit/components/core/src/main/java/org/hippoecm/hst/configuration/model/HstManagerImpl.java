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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.StringPool;
import org.hippoecm.hst.configuration.channel.MutableChannelManager;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerRegistry;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the loading of {@link HstNodeImpl}'s. 
 */
public class HstManagerImpl implements MutableHstManager {
    
    private static final Logger log = LoggerFactory.getLogger(HstManagerImpl.class);

    /**
     * general mutex on which implementation can synchronize with the HstManagerImpl
     */
    public static final Object MUTEX = new Object();

    private Repository repository;
    private Credentials credentials;

    private volatile VirtualHosts virtualHosts;

    // stale model with async loading support instance variables
    private static final Executor executor = Executors.newSingleThreadExecutor();
    boolean staleConfigurationSupported = false;
    private volatile VirtualHosts staleVirtualHosts;
    private volatile boolean virtualHostsBeingBuild = false;
    private volatile boolean virtualHostsBuildAlreadyScheduled = false;
    // end stale model with async loading support instance variables

    private HstURLFactory urlFactory;
    private HstSiteMapMatcher siteMapMatcher;
    private HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;
    
    private HstComponentRegistry componentRegistry;
    private HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry;
    private HstCache pageCache;
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
     * Contains the map of all siteRootNodes of the previously loaded model : This can be used to validate the
     * newly loaded set of siteRootNodes against the previously loaded ones: Due to jcr changes during finegrained
     * reloading of the model, it can sometimes happen that a siteRootNode cannot load its configuration path because
     * the configuration node has been moved after the siteNode was loaded. When this happens, it can have two causes:
     * 1) The hst configuration is really broken
     * 2) In a clustered repository, the repository pulled in some changes during reloading of the model
     * Now, case (1) cannot be fixed of course. Case (2) we try to identify through this previousCorrectLoadedSiteRootNodePaths :
     * if some siteRoot node has a broken configuration path, but was correctly loaded before because it was part of
     * previousCorrectLoadedSiteRootNodePaths, then we do a fullblown rebuild
     */
    private Set<String> previousCorrectLoadedSiteRootNodePaths = new HashSet<String>();

    /**

     * Request path suffix delimiter
     */
    private String pathSuffixDelimiter = "./";

    private String[] hstFilterPrefixExclusions;
    private String[] hstFilterSuffixExclusions;
    
    /**
     * The list of implicit configuration augmenters which can provide extra hst configuration after the {@link VirtualHosts} object 
     * has been created
     */
    List<HstConfigurationAugmenter> hstConfigurationAugmenters = new ArrayList<HstConfigurationAugmenter>();

    // this member is only accessed in synchronized blocks so does not need to be volatile
    private boolean clearAll = false;

    private RelevantEventsHolder relevantEventsHolder;
    
    private MutableChannelManager channelManager;

    public synchronized void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public synchronized void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
    
    public synchronized void setRootPath(String rootPath) {
        this.rootPath = rootPath;
        rootPathDepth = rootPath.split("/").length;
        relevantEventsHolder = new RelevantEventsHolder(rootPath);
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

    public void setPageCache(HstCache pageCache) {
        this.pageCache = pageCache;
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

    public void setHstFilterPrefixExclusions(final String[] hstFilterPrefixExclusions) {
        this.hstFilterPrefixExclusions = hstFilterPrefixExclusions;
    }

    public void setHstFilterSuffixExclusions(final String[] hstFilterSuffixExclusions) {
        this.hstFilterSuffixExclusions = hstFilterSuffixExclusions;
    }

    public void setStaleConfigurationSupported(boolean staleConfigurationSupported) {
        this.staleConfigurationSupported = staleConfigurationSupported;
    }

    public boolean isExcludedByHstFilterInitParameter(String pathInfo) {
        if (hstFilterPrefixExclusions != null) {
            for(String excludePrefix : hstFilterPrefixExclusions) {
                if(pathInfo.startsWith(excludePrefix)) {
                    log.debug("pathInfo '{}' is excluded by init parameter containing excludePrefix '{}'", pathInfo, excludePrefix);
                    return true;
                }
            }
        }
        if (hstFilterSuffixExclusions != null) {
            for(String excludeSuffix : hstFilterSuffixExclusions) {
                if(pathInfo.endsWith(excludeSuffix)) {
                    log.debug("pathInfo '{}' is excluded by init parameter containing excludeSuffix '{}'", pathInfo, excludeSuffix);
                    return true;
                }
            }
        }
        return false;
    }
    
    public VirtualHosts getVirtualHosts(boolean allowStale) throws ContainerException {
        if (allowStale && staleConfigurationSupported) {
            VirtualHosts currentHosts = virtualHosts;
            if (currentHosts != null) {
                return currentHosts;
            }
            VirtualHosts staleHosts = staleVirtualHosts;
            if (staleHosts != null) {
                if (!virtualHostsBeingBuild && !virtualHostsBuildAlreadyScheduled) {
                    virtualHostsBuildAlreadyScheduled = true;
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                getVirtualHosts();
                            } catch (ContainerException e) {
                                log.warn("Exception during building virtualhosts model. ", e);
                            } finally {
                                // finished with the scheduled job
                                virtualHostsBuildAlreadyScheduled = false;
                            }
                        }
                    });
                } else {
                    log.debug("Returning stale virtualhosts model and no async build will be done because already being" +
                            "build or scheduled.");
                }
                return staleHosts;
            }
            return getVirtualHosts();
        } else {
            return getVirtualHosts();
        }
    }
    
    public VirtualHosts getVirtualHosts() throws ContainerException {

        VirtualHosts currentHosts = virtualHosts;
        if (currentHosts == null) {
            synchronized(MUTEX) {
                if (virtualHosts == null) {
                    try {
                        virtualHostsBeingBuild = true;
                        try {
                            buildSites();
                        } catch (ModelLoadingException e) {
                            // since the model has changed during loading, we try a complete rebuild (invalidateAll). If that one fails again, we return a ContainerException
                            // and it will be tried during the next request
                            if (log.isDebugEnabled()) {
                                log.warn("Model was possibly not build correctly. A total rebuild will be done now after flushing all caches.", e);
                            } else {
                                log.warn("Model was possibly not build correctly. A total rebuild will be done now after flushing all caches. Reason : {} ", e.toString());
                            }
                            retryCleanBuildSites();
                        } catch (ContainerException e) {
                            if (log.isDebugEnabled()) {
                                log.warn("During building the HST model an error occured. A total rebuild will be done now after flushing all caches.", e);
                            } else {
                                log.warn("During building the HST model an error occured. A total rebuild will be done now after flushing all caches. Reason : {} ", e.toString());
                            }
                            retryCleanBuildSites();
                        }
                    } catch (LoginException e) {
                        throw new ContainerException("Could not build hst model because user 'configuser' could not login to the repository.");
                    } finally {
                        virtualHostsBeingBuild = false;
                    }
                    pageCache.clear();
                }
                currentHosts = virtualHosts;
                log.info("Flushing page cache after new model is loaded");
            }
        }
        return currentHosts;
    }

    private void retryCleanBuildSites() throws ContainerException, LoginException {
        invalidateAll();
        try {
            buildSites();
        } catch (ModelLoadingException e) {
            invalidateAll();
            throw new ContainerException("Retry of building HST model due to jcr config changes during the previous build failed again " +
                    "because the jcr config changed again. No retry done untill next request.");
        }
    }

    private void buildSites() throws ContainerException, LoginException {
        log.info("Start building in memory hst configuration model");
        long start = System.currentTimeMillis();
        
        if (clearAll) {
            virtualHostsNode = null;
            commonCatalog = null;
            configurationRootNodes.clear();
            siteRootNodes.clear();
            previousCorrectLoadedSiteRootNodePaths.clear();
            relevantEventsHolder.clear();
        }

        Session session = null;
        if (log.isDebugEnabled()) {
            logRelevantEventsHolder();
        }
        try {
            if (this.credentials == null) {
                session = this.repository.login();
            } else {
                session = this.repository.login(this.credentials);
            }
            session.refresh(false);
            // get all the root hst virtualhosts node: there is only allowed to be exactly ONE

            if(virtualHostsNode == null) {
                Node virtualHostsJcrNode = session.getNode(rootPath + "/hst:hosts");
                virtualHostsNode = new HstNodeImpl(virtualHostsJcrNode, null, true);
            } else if (relevantEventsHolder.hasHostEvents()){
                Node virtualHostsJcrNode = session.getNode(rootPath + "/hst:hosts");
                virtualHostsNode = new HstNodeImpl(virtualHostsJcrNode, null, true);
            }

            // if there is a common catalog that is not yet loaded, we load this one:
            if(relevantEventsHolder.hasCommonCatalogEvents()) {
                commonCatalog = null;
                if (session.itemExists(rootPath +"/hst:configurations/hst:catalog")) {
                    // we have a common catalog. Load this catalog. It is available for every (sub)site
                    Node catalog = (Node)session.getItem(rootPath +"/hst:configurations/hst:catalog");
                    commonCatalog = new HstNodeImpl(catalog, null, true);
                }
            } 

            if(configurationRootNodes.isEmpty()) {
                loadAllConfigurationNodes(session);
                hstComponentsConfigurationInstanceCache.clear();
                componentRegistry.unregisterAllComponents();
            } else if (relevantEventsHolder.hasHstRootConfigurationEvents()) {
                hstComponentsConfigurationInstanceCache.clear();
                componentRegistry.unregisterAllComponents();
                Iterator<String> hstConfigurationPathEvents = relevantEventsHolder.getHstRootConfigurationPathEvents();
                while(hstConfigurationPathEvents.hasNext()) {
                    String path = hstConfigurationPathEvents.next();
                    configurationRootNodes.remove(path);
                    if(session.nodeExists(path)) {
                         HstNode configurationRootNode = new HstNodeImpl(session.getNode(path), null, true);
                        configurationRootNodes.put(path, configurationRootNode);
                    }
                }

            }

            // get all the hst:site's 
            if (siteRootNodes.isEmpty()) {
                loadAllSiteNodes(session);
            } else if (relevantEventsHolder.hasSiteEvents()) {
                Iterator<String> siteRootPathEvents = relevantEventsHolder.getRootSitePathEvents();
                while(siteRootPathEvents.hasNext()) {
                    String path = siteRootPathEvents.next();
                    String[] elems = path.split("/");
                    // check if it is a node of types hst:sites : in this case, we'll reload all sites
                    if (elems.length == rootPathDepth + 1) {
                        // change in hst:sites. clear all siteRootNodes
                        siteRootNodes.clear();
                        loadAllSiteNodes(session);
                        break;
                    } else {
                        siteRootNodes.remove(path);
                        if(session.nodeExists(path)) {
                            Node rootSiteNode = session.getNode(path);
                            if(rootSiteNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITE)) {
                                HstSiteRootNode hstSiteRootNode = new HstSiteRootNodeImpl(rootSiteNode, null, getRootHstConfigurationsPath());
                                siteRootNodes.put(path, hstSiteRootNode);
                            } else {
                                throw new IllegalStateException(String.format("Unexpected nodetype '%s' for '%s'. Can only load nodes" +
                                        " of type '%s' for sites.", rootSiteNode.getPrimaryNodeType().getName(), rootSiteNode.getPath(), HstNodeTypes.NODETYPE_HST_SITE));
                            }
                        }
                    }
                }
            }

            removeInvalidSiteRootNodes(session);
            populatePreviousCorrectLoadedSiteRootNodePaths();

            try {

                siteMapItemHandlerRegistry.unregisterAllSiteMapItemHandlers();
                enhancedConfigurationRootNodes = enhanceHstConfigurationNodes(configurationRootNodes);
                this.virtualHosts = new VirtualHostsService(virtualHostsNode, this);
                for(HstConfigurationAugmenter configurationAugmenter : hstConfigurationAugmenters ) {
                    configurationAugmenter.augment(this);
                }
                log.info("Finished build in memory hst configuration model in " + (System.currentTimeMillis() - start) + " ms.");
            } catch (ServiceException e) {
                throw new ContainerException("Exception during building HST model", e);
            }

            this.channelManager.load(virtualHosts, session);
            if (staleConfigurationSupported) {
                staleVirtualHosts = virtualHosts;
            }
        } catch (LoginException e) {
            throw e;
        } catch (PathNotFoundException e) {
            throw new ContainerException("PathNotFoundException during building HST model", e);
        } catch (RepositoryException e) {
            if (e.getCause() instanceof LoginException) {
                // because LazyMultipleRepositoryImpl#login wraps any exception during login in a RepositoryException,
                // we need to inspect the cause
                throw (LoginException)e.getCause();
            }
            throw new ContainerException("RepositoryException during building HST model", e);
        } catch (RuntimeRepositoryException e) {
            throw new ContainerException("RepositoryException during building HST model", e);
        } finally {
            // clear the StringPool as it is not needed any more
            StringPool.clear();
            enhancedConfigurationRootNodes.clear();
            relevantEventsHolder.clear();
            hstLinkCreator.clear();
            clearAll = false;

            if (session != null) {
                try { 
                    session.logout(); 
                } catch (Exception e) {
                    log.warn("Exception during loggin out jcr session", e);
                }
            }
        }

    }

    private void populatePreviousCorrectLoadedSiteRootNodePaths() {
        previousCorrectLoadedSiteRootNodePaths.clear();
        for (String siteRootNodePath : siteRootNodes.keySet()) {
            previousCorrectLoadedSiteRootNodePaths.add(siteRootNodePath);
        }
    }

    private void removeInvalidSiteRootNodes(final Session session) throws RepositoryException {
        List<String> invalidSiteRootNodePaths = new ArrayList<String>();
        for (HstSiteRootNode hstSiteRootNode : siteRootNodes.values()) {
            if (!containsConfigurationRootNode(session, hstSiteRootNode)) {
                HstNode configurationRootNode = tryLoadConfigurationForSite(session, hstSiteRootNode);
                if (configurationRootNode == null) {
                    // the site really can't be loaded correctly. If it was loaded correctly before, then most
                    // likely we had jcr node changes in a clustered environment during model loading
                    String siteRootNodePath = hstSiteRootNode.getValueProvider().getPath();
                    failIfLoadedCorrectlyBefore(siteRootNodePath);
                    invalidSiteRootNodePaths.add(hstSiteRootNode.getValueProvider().getPath());
                } else {
                    configurationRootNodes.put(hstSiteRootNode.getConfigurationPath(), configurationRootNode);
                }
            }
        }
        for (String invalidSiteRootNodePath : invalidSiteRootNodePaths) {
            log.warn("Discarding invalid HST SITE for '{}'.", invalidSiteRootNodePath);
            siteRootNodes.remove(invalidSiteRootNodePath);
        }
    }

    private void failIfLoadedCorrectlyBefore(final String siteRootNodePath) {
        if (previousCorrectLoadedSiteRootNodePaths.isEmpty()) {
           return;
        }
        if (previousCorrectLoadedSiteRootNodePaths.contains(siteRootNodePath)) {
            throw new ModelLoadingException("Found HST SITE '"+siteRootNodePath+"' that does not have a " +
                    "configuration node. However, it was loaded correctly before. ");
        }
    }

    private HstNode tryLoadConfigurationForSite(final Session session, final HstSiteRootNode hstSiteRootNode) throws RepositoryException {
        session.refresh(false);
        String configurationPath = hstSiteRootNode.getConfigurationPath();
        if(session.nodeExists(configurationPath)) {
            Node jcrNode = session.getNode(configurationPath);
            if (!jcrNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
                log.warn("Hst configuration node at '{}' for site '{}' is not of correct type. Discarding this hst:site from the model now.", configurationPath, hstSiteRootNode.getValueProvider().getPath());
                return null;
            }
            return new HstNodeImpl(jcrNode, null, true);
        } else {
            return null;
        }
    }

    private boolean containsConfigurationRootNode(final Session session, final HstSiteRootNode hstSiteRootNode) throws RepositoryException {
        String configurationPath = hstSiteRootNode.getConfigurationPath();
        if (configurationRootNodes.containsKey(configurationPath)) {
            return true;
        } else {
            log.debug("Configuration path '{}' not yet loaded. This might happen because jcr event not yet arrived to indicate that " +
                    "a new config node is present. Try loading it now.");
            return false;
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

    private void loadAllConfigurationNodes(Session session) throws RepositoryException {
        Node configurationsNode = session.getNode(rootPath + "/hst:configurations");
        NodeIterator configurationRootJcrNodes = configurationsNode.getNodes();
        long iteratorSizeBeforeLoop = configurationRootJcrNodes.getSize();
        while(configurationRootJcrNodes.hasNext()) {
            Node configurationRootNode = configurationRootJcrNodes.nextNode();
            if(configurationRootNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
                HstNode hstNode = new HstNodeImpl(configurationRootNode, null, true);
                configurationRootNodes.put(hstNode.getValueProvider().getPath(), hstNode);
            }
        }
        long iteratorSizeAfterLoop = configurationRootJcrNodes.getSize();
        throwModelChangedExceptionIfCountsAreNotEqual(iteratorSizeBeforeLoop, iteratorSizeAfterLoop);
    }

    private void loadAllSiteNodes(Session session) throws RepositoryException {
        NodeIterator nodes = session.getNode(rootPath).getNodes();
        long iteratorSizeBeforeLoop = nodes.getSize();
        while(nodes.hasNext()) {
            Node node = nodes.nextNode();
            if(node.isNodeType(HstNodeTypes.NODETYPE_HST_SITES)) {
                NodeIterator siteRootJcrNodes = node.getNodes();
                while(siteRootJcrNodes.hasNext()) {
                    Node rootSiteNode = siteRootJcrNodes.nextNode();
                    HstSiteRootNode hstSiteRootNode = new HstSiteRootNodeImpl(rootSiteNode, null, getRootHstConfigurationsPath());
                    siteRootNodes.put(hstSiteRootNode.getValueProvider().getPath(), hstSiteRootNode);
                }
            }
        }
        long iteratorSizeAfterLoop = nodes.getSize();
        throwModelChangedExceptionIfCountsAreNotEqual(iteratorSizeBeforeLoop, iteratorSizeAfterLoop);
    }

    private String getRootHstConfigurationsPath() {
        return rootPath +"/hst:configurations";
    }

    public static void throwModelChangedExceptionIfCountsAreNotEqual(final long iteratorSizeBeforeLoop, final long iteratorSizeAfterLoop) {
        // typically, in jackrabbit (clustering) it is not uncommon that a jcr iterator at initialization contains nodes that
        // get deleted before actually fetched through the LazyItemIterator. This can be detected by a different size after
        // the iteration than before the iteration
        if (iteratorSizeBeforeLoop != iteratorSizeAfterLoop) {
            throw new ModelLoadingException("During building the in memory HST model, the hst configuration jcr nodes have changed.");
        }
    }

    @Override
    public void invalidate(EventIterator events) {
        synchronized(MUTEX) {
            try {
                while (events.hasNext()) {
                    relevantEventsHolder.addEvent(events.nextEvent());
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException happened during processing events. Invalidate hst model completely", e);
                invalidateAll();
                return;
            }
            invalidateVirtualHosts();
        }
    }

    @Override
    public void invalidateAll() {
        synchronized(MUTEX) {
            invalidateVirtualHosts();
            clearAll = true;
        }
    }

    private final void invalidateVirtualHosts() {
        log.info("In memory hst configuration model is invalidated. It will be reloaded on the next request.");
        if (virtualHosts != null && staleConfigurationSupported) {
            staleVirtualHosts = virtualHosts;
        }
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

    public void logRelevantEventsHolder() {
        if (relevantEventsHolder.hasEvents()) {
            log.debug("--------- Relevant events ----------- ");
            final Iterator<String> hostPathEvents = relevantEventsHolder.getHostPathEvents();
            while(hostPathEvents.hasNext()) {
                log.debug("HOST PATH EVENT: {}", hostPathEvents.next());
            }
            Iterator<String> hstConfigurationPathEvents = relevantEventsHolder.getHstRootConfigurationPathEvents();
            while(hstConfigurationPathEvents.hasNext()) {
                log.debug("CONFIGURATION EVENT: {}", hstConfigurationPathEvents.next());
            }
            Iterator<String> siteRootPathEvents = relevantEventsHolder.getRootSitePathEvents();
            while(siteRootPathEvents.hasNext()) {
                log.debug("SITE EVENT: {}", siteRootPathEvents.next());
            }
            Iterator<String> commonCatalogPathEvents = relevantEventsHolder.getCommonCatalogPathEvents();
            while(commonCatalogPathEvents.hasNext()) {
                log.debug("COMMON CATALOG EVENT: {}", commonCatalogPathEvents.next());
            }
            log.debug("--------- End relevant events ----------- ");
        }
    }
}
