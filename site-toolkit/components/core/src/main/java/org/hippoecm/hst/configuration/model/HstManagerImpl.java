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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.configuration.HstNodeTypes;
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
            
            // if there is a common catalog, we load this one:
            if(session.itemExists(rootPath +"/hst:configurations/hst:catalog")) {
                // we have a common catalog. Load this catalog. It is available for every (sub)site
                Node catalog = (Node)session.getItem(rootPath +"/hst:configurations/hst:catalog");
                commonCatalog = new HstNodeImpl(catalog, null, true);
            } 
            
            // get all the root hst configuration nodes
            {
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
                            configurationRootNodes.put(configurationRootNode.getPath(), hstNode);
                        } catch (HstNodeException e) {
                            log.error("Exception while creating Hst configuration for '"+configurationRootNode.getPath()+"'. Fix configuration" ,e);
                        }
                        
                    }
                }
            }
            
            // get all the mount points
            String xpath = "/jcr:root"+rootPath+"//element(*, "+HstNodeTypes.NODETYPE_HST_SITE+")  order by @jcr:score descending ";
            QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator siteRootJcrNodes = result.getNodes();
            
            while(siteRootJcrNodes.hasNext()) {
                Node rootSiteNode = siteRootJcrNodes.nextNode();
                HstSiteRootNode hstSiteRootNode = new HstSiteRootNodeImpl(rootSiteNode, null);
                siteRootNodes.put(hstSiteRootNode.getValueProvider().getPath(), hstSiteRootNode);
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
            this.virtualHosts = new VirtualHostsService(virtualHostsNode, this);
        } catch (ServiceException e) {
            throw new RepositoryNotAvailableException(e);
        }
    }
    
    public void invalidate(String path) {
        virtualHosts = null;
        commonCatalog = null;
        configurationRootNodes.clear();
        siteRootNodes.clear();
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

    public String getPathSuffixDelimiter() {
        return pathSuffixDelimiter;
    }
    
    public void setPathSuffixDelimiter(String pathSuffixDelimiter) {
        this.pathSuffixDelimiter = pathSuffixDelimiter;
    }
}
