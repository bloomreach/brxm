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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.StringPool;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.channel.ChannelManagerImpl;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerRegistry;
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
    
    /**
     * This is a temporal cache only used during building the hst config model: When all the backing HstNode's for 
     * hst:pages, hst:components, hst:catalog and hst:templates, then, the HstComponentsConfiguration object can be shared between different Mounts.
     * The key is the Set of all HstNode path's directly below the components, pages, catalog and templates : The path uniquely defines the HstNode
     * and there is only inheritance on the nodes directly below components, pages, catalog and templates: Since no fine-grained inheritance, these
     * HstNode's identify uniqueness 
     */
    private Map<Set<String>, HstComponentsConfigurationService> tmpHstComponentsConfigurationInstanceCache;
    
    
    /**
     * The root path of all the hst configuations nodes, by default /hst:hst
     */
    private String rootPath;
   
    /**
     * The root of the virtual hosts node. There should always be exactly one.
     */
    private HstNode virtualHostsNode; 
    
    /**
     * The common catalog node and <code>null</code> if there is no common catalog (hst:configurations/hst:catalog)
     */
    private HstNode commonCatalog;

    /**
     * The map of all configurationRootNodes where the key is the path to the configuration
     */
    private Map<String, HstNode> configurationRootNodes = new HashMap<String, HstNode>();

    /**
     * The map of all site nodes where the key is the path
     */
    private Map<String, HstSiteRootNode> siteRootNodes = new HashMap<String, HstSiteRootNode>();
    
    /**
     * Request path suffix delimiter
     */
    private String pathSuffixDelimiter = "./";
    

    private boolean clearAll = false;
    private Set<String> loadOrReloadHstConfigurationSet= Collections.synchronizedSet(new HashSet<String>());
    private Set<String> loadOrReloadHstHostSet = Collections.synchronizedSet(new HashSet<String>());
    private Set<String> loadOrReloadHstSiteSet = Collections.synchronizedSet(new HashSet<String>());
    private ChannelManager channelManager;

    public synchronized void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public synchronized void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
    
    public synchronized void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
    
    public void setComponentRegistry(HstComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }
    
    public void setSiteMapItemHandlerRegistry(HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry) {
        this.siteMapItemHandlerRegistry = siteMapItemHandlerRegistry;
    }
    
    public synchronized String getRootPath() {
        return rootPath;
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
    
    public void setSiteMapItemHandlerFactory(HstSiteMapItemHandlerFactory siteMapItemHandlerFactory) {
        this.siteMapItemHandlerFactory = siteMapItemHandlerFactory;
    }
    
    public HstSiteMapItemHandlerFactory getSiteMapItemHandlerFactory() {
        return siteMapItemHandlerFactory;
    }
    
    public VirtualHosts getVirtualHosts() throws RepositoryNotAvailableException {
        if (virtualHosts == null) {
            synchronized(this) {
                if (virtualHosts == null) {
                    buildSites();
                    // when we have a new virtualhosts object, clear all registries
                    componentRegistry.unregisterAllComponents();
                    siteMapItemHandlerRegistry.unregisterAllSiteMapItemHandlers();
                }
            }
        }
        
        return virtualHosts;
    }

    protected void buildSites() throws RepositoryNotAvailableException{
        if(clearAll) { 
            loadOrReloadHstConfigurationSet.clear();
            loadOrReloadHstHostSet.clear();
            loadOrReloadHstSiteSet.clear();
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
                String xpath = "/jcr:root"+rootPath+"//element(*, "+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS+") order by @jcr:score descending ";
                QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
                
                NodeIterator virtualHostNodes = result.getNodes();
                if(virtualHostNodes.getSize() != 1L) {
                    throw new RepositoryNotAvailableException("There must be exactly one node of type '"+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS+"' but there are "+virtualHostNodes.getSize()+" .");
                }
                // there is exactly one virtualHostsNode
                Node virtualHostsJcrNode = virtualHostNodes.nextNode();

                virtualHostsNode = new HstNodeImpl(virtualHostsJcrNode, null, true);
            } 
            
            // if there is a common catalog that is not yet loaded, we load this one:
            if(commonCatalog == null && session.itemExists(rootPath +"/hst:configurations/hst:catalog")) {
                // we have a common catalog. Load this catalog. It is available for every (sub)site
                Node catalog = (Node)session.getItem(rootPath +"/hst:configurations/hst:catalog");
                commonCatalog = new HstNodeImpl(catalog, null, true);
            } 
 
            // get all the root hst configuration nodes
            { 
                if(configurationRootNodes.isEmpty()) {
                    String xpath = "/jcr:root"+rootPath+"//element(*, "+HstNodeTypes.NODETYPE_HST_CONFIGURATION+")  order by @jcr:score descending ";
                    QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
                    NodeIterator configurationRootJcrNodes = result.getNodes();
    
                    while(configurationRootJcrNodes.hasNext()) {
                        Node configurationRootNode = configurationRootJcrNodes.nextNode();
                        if(configurationRootNode.getName().equals(HstNodeTypes.NODENAME_HST_HSTDEFAULT)) {
                            // the hstdefault is only meant for 'implicit inheriting'. We can skip it here
                        } else {
                            if(configurationRootNodes.containsKey(configurationRootNode.getPath())) {
                                // already loaded, for example because inherited configs can already be loaded through HstSiteConfigurationRootNodeImpl
                                continue;
                            }
                            try {
                                HstNode hstNode = new HstSiteConfigurationRootNodeImpl(configurationRootNode, null, this);
                                configurationRootNodes.put(hstNode.getValueProvider().getPath(), hstNode);
                               
                            } catch (HstNodeException e) {
                                log.error("Exception while creating Hst configuration for '"+configurationRootNode.getPath()+"'. Fix configuration" ,e);
                            }
                            
                        }
                    }
                } else {
                    // only reload the change configurations 
                    for(String configurationNode : loadOrReloadHstConfigurationSet) {
                        if(!session.itemExists(configurationNode)) {
                            continue;
                        }
                        Node configurationRootNode = session.getNode(configurationNode);
                        if(configurationRootNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
                            HstNode hstNode = new HstSiteConfigurationRootNodeImpl(configurationRootNode, null, this);
                            configurationRootNodes.put(hstNode.getValueProvider().getPath(), hstNode);
                        }
                    }
                }
            }
            
            // get all the hst:site's
            if(siteRootNodes.isEmpty()) {
                String xpath = "/jcr:root"+rootPath+"//element(*, "+HstNodeTypes.NODETYPE_HST_SITE+")  order by @jcr:score descending ";
                QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
                NodeIterator siteRootJcrNodes = result.getNodes();
                
                while(siteRootJcrNodes.hasNext()) {
                    Node rootSiteNode = siteRootJcrNodes.nextNode();
                    HstSiteRootNode hstSiteRootNode = new HstSiteRootNodeImpl(rootSiteNode, null);
                    siteRootNodes.put(hstSiteRootNode.getValueProvider().getPath(), hstSiteRootNode);
                }
            } else {
                // only reload the changed sites 
                for(String siteNode : loadOrReloadHstSiteSet) {
                    if(!session.itemExists(siteNode)) {
                        continue;
                    }
                    Node siteRootNode = session.getNode(siteNode);
                    if(siteRootNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITE)) {
                        HstSiteRootNode hstSiteRootNode = new HstSiteRootNodeImpl(siteRootNode, null);
                        siteRootNodes.put(hstSiteRootNode.getValueProvider().getPath(), hstSiteRootNode);
                    }
                }
            }
            
            
        } catch (RepositoryException e) {
            throw new RepositoryNotAvailableException("Exception during loading configuration nodes. ",e);
        } finally {
            if (session != null) {
                try { 
                    session.logout(); 
                } catch (Exception ce) {
                    throw new RepositoryNotAvailableException("Exception while loging out jcr session ",ce);
                }
            }
        }
         
        try {
            tmpHstComponentsConfigurationInstanceCache = new HashMap<Set<String>, HstComponentsConfigurationService>();
            this.virtualHosts = new VirtualHostsService(virtualHostsNode, this);
        } catch (ServiceException e) {
            throw new RepositoryNotAvailableException(e);
        } finally {
            // we are finished with rebuild the hst model: Set the temporary cache to null. 
            tmpHstComponentsConfigurationInstanceCache = null;
            // clear the StringPool as it is not needed any more
            StringPool.clear();
            loadOrReloadHstConfigurationSet.clear();
            loadOrReloadHstHostSet.clear();
            loadOrReloadHstSiteSet.clear();
            clearAll = false;
        }
    }
    
    @Override
    public void invalidate(EventIterator events) {
        
        // TODO remove this clearAll = true here: this is a shortcut to mimic the old, non finegrained reloading 
        // as this finegrained reloading is not yet completely done
        clearAll = true;
        if(clearAll) {
            // TODO remove this part and use code below with finegrained invalidation
            virtualHosts = null;
            return;
        }
        
        synchronized(this) {
            /*
             * below, we are going to prepare which HstNode's should be reloaded in our model. 
             * Depending on the change in the jcr node in the hst configuration we will mark either:
             * 
             * 1) To reload some specific entire hst:site node + descendants
             * 2) To reload some specific entire hst:configuration node + descendants
             * 3) To reload the entire hst:hosts + descendants
             */
            
            // below we first collide all events for the same nodes to one eventPath
            
            // all nodes that have been added or moved or have a property added/changed/removed
            Map<String, String> idPathMapOfChangedAddedOrMovedNodes = new HashMap<String, String>();
            // all removed nodes
            Map<String, String> idPathMapOfRemovedNodes = new HashMap<String, String>();
            
            try {
                while (events.hasNext()) {
                    Event ev = events.nextEvent();
                    
                    switch (ev.getType()) {
                        case Event.NODE_ADDED:
                            if(idPathMapOfChangedAddedOrMovedNodes.containsKey(ev.getIdentifier())) {
                                break;
                            }
                            idPathMapOfChangedAddedOrMovedNodes.put(ev.getIdentifier(), ev.getPath());
                            break;
                        case Event.NODE_REMOVED:
                            idPathMapOfRemovedNodes.put(ev.getIdentifier(), ev.getPath());
                            break;
                        case Event.NODE_MOVED:
                            if(idPathMapOfChangedAddedOrMovedNodes.containsKey(ev.getIdentifier())) {
                                break;
                            }
                            break;
                        case Event.PROPERTY_ADDED:
                            addNodePathIfAbsentForPropertyToMap(idPathMapOfChangedAddedOrMovedNodes, ev);
                            break;
                        case Event.PROPERTY_CHANGED:
                            addNodePathIfAbsentForPropertyToMap(idPathMapOfChangedAddedOrMovedNodes, ev);
                            break;
                        case Event.PROPERTY_REMOVED:
                            addNodePathIfAbsentForPropertyToMap(idPathMapOfChangedAddedOrMovedNodes, ev);
                            break;
                      }
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException happened. Invalidate hst model completely", e);
                virtualHosts = null;
                clearAll = true;
                return;
            }
            
            // now that we have the changed and deleted maps, let's compute which hst:configuration nodes need to be reloaded,
            // which hst:sites and which hst:hosts

            String hstConfigPath = rootPath+"/hst:configurations/";
            String hstCommonCatalogPath = hstConfigPath+"/hst:catalog/";
            String hstHostsPath = rootPath+"/hst:hosts/";
            String hstBlueprintsPath = rootPath+"/hst:blueprints/";
            for(String path : idPathMapOfChangedAddedOrMovedNodes.values()) {
                if(path.startsWith(hstConfigPath)) {
                    String pathOfConfigurationNode = path.substring(hstConfigPath.length());
                    pathOfConfigurationNode = hstConfigPath + pathOfConfigurationNode.substring(0, pathOfConfigurationNode.indexOf("/"));
                    loadOrReloadHstConfigurationSet.add(pathOfConfigurationNode);
                } else if(path.startsWith(hstCommonCatalogPath)) {
                    // this is the common catalog: set that one to null
                    commonCatalog = null;
                } else if (path.startsWith(hstHostsPath)) {
                    // for now reload all hosts. Can be improved by finegrained invalidation
                    virtualHostsNode = null;
                } else if (path.startsWith(hstBlueprintsPath)) {
                    // do nothing
                } else {
                    // it must be a change in a hst:site or in hst:hst, or in hst:hosts, hst:sites or hst:configurations
                    String[] elems = path.split("/");
                    if (elems.length < 4) {
                        // ignore for now.
                    } else {
                        String siteNodePath = "/" + elems[1] + "/" + elems[2] + "/" + elems[3];
                        loadOrReloadHstSiteSet.add(siteNodePath);
                    }
                } 
            }
            // set the model to null
            virtualHosts = null;
        }
    }
    
    private void addNodePathIfAbsentForPropertyToMap(Map<String, String> idPathMapOfChangedAddedOrMovedNodes, Event ev) throws RepositoryException {
        if(idPathMapOfChangedAddedOrMovedNodes.containsKey(ev.getIdentifier())) {
            return;
        } else {
            String propertyPath = ev.getPath();
            String nodePath = propertyPath.substring(0,propertyPath.lastIndexOf("/"));
            idPathMapOfChangedAddedOrMovedNodes.put(ev.getIdentifier(), nodePath);
        }
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
            virtualHosts = null;
            clearAll = true;
        }
    }
    
    public Map<String, HstSiteRootNode> getHstSiteRootNodes(){
        return siteRootNodes;
    }

    public Map<String, HstNode> getConfigurationRootNodes() {
        return configurationRootNodes;
    }
    
    public HstNode getCommonCatalog(){
        return commonCatalog;
    }
    
    public Map<Set<String>, HstComponentsConfigurationService> getTmpHstComponentsConfigurationInstanceCache() {
        return tmpHstComponentsConfigurationInstanceCache;
    }

    public String getPathSuffixDelimiter() {
        return pathSuffixDelimiter;
    }
    
    public void setPathSuffixDelimiter(String pathSuffixDelimiter) {
        this.pathSuffixDelimiter = pathSuffixDelimiter;
    }


    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }
}
