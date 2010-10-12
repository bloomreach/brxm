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
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
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

    private VirtualHosts virtualHosts;
    private HstURLFactory urlFactory;
    private HstSiteMapMatcher siteMapMatcher;
    private HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;
    
    /**
     * The root path of all the hst configuations nodes, by default /hst:hst
     */
    private String rootPath;
   
    /**
     * The root of the virtual hosts node. There should always be exactly one.
     */
    HstNode virtualHostsNode; 

    /**
     * The map of all configurationRootNodes where the key is the path to the configuration
     */
    Map<String, HstNode> configurationRootNodes = new HashMap<String, HstNode>();

    /**
     * The map of all site nodes where the key is the path
     */
    Map<String, HstSiteRootNode> siteRootNodes = new HashMap<String, HstSiteRootNode>();
    
    /**
     * Request path suffix delimiter
     */
    private String pathSuffixDelimiter = "./";
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
    
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
    
    public String getRootPath() {
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
    
    public VirtualHosts getVirtualHosts() throws RepositoryNotAvailableException{
        VirtualHosts vHosts = this.virtualHosts;
        
        if (vHosts == null) {
            synchronized(this) {
                buildSites();
                vHosts = this.virtualHosts;
            }
        }
        
        return vHosts;
    }
    
    protected synchronized void buildSites() throws RepositoryNotAvailableException{
        if (this.virtualHosts != null) {
            return;
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
                String xpath = "/jcr:root"+rootPath+"//element(*, "+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS+")";
                QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
                
                NodeIterator virtualHostNodes = result.getNodes();
                if(virtualHostNodes.getSize() != 1L) {
                    throw new RepositoryNotAvailableException("There must be exactly one node of type '"+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS+"' but there are "+virtualHostNodes.getSize()+" .");
                }
                // there is exactly one virtualHostsNode
                Node virtualHostsJcrNode = virtualHostNodes.nextNode();
                virtualHostsNode = new HstNodeImpl(virtualHostsJcrNode, null, true);
            } 
            
            
            // get all the root hst configuration nodes
            {
                String xpath = "/jcr:root"+rootPath+"//element(*, "+HstNodeTypes.NODETYPE_HST_CONFIGURATION+")";
                QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
                NodeIterator configurationRootJcrNodes = result.getNodes();
                
                while(configurationRootJcrNodes.hasNext()) {
                    Node configurationRootNode = configurationRootJcrNodes.nextNode();
                    HstNode hstNode = new HstNodeImpl(configurationRootNode, null, true);
                    configurationRootNodes.put(hstNode.getValueProvider().getPath(), hstNode);
                }
            }
            
            // get all the mount points
            String xpath = "/jcr:root"+rootPath+"//element(*, "+HstNodeTypes.NODETYPE_HST_SITE+")";
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
            this.virtualHosts = new VirtualHostsService(getVirtualHostsNode(), this);
        } catch (ServiceException e) {
            throw new RepositoryNotAvailableException(e);
        }
    }
    
    public synchronized void invalidate(String path) {
        this.virtualHosts = null;
    }
    
    
    
    public HstNode getVirtualHostsNode() {
        return virtualHostsNode;
    }
    
    public Map<String, HstSiteRootNode> getHstSiteRootNodes(){
        return siteRootNodes;
    }

    public Map<String, HstNode> getConfigurationRootNodes() {
        return configurationRootNodes;
    }

    public String getPathSuffixDelimiter() {
        return pathSuffixDelimiter;
    }
    
    public void setPathSuffixDelimiter(String pathSuffixDelimiter) {
        this.pathSuffixDelimiter = pathSuffixDelimiter;
    }
}
