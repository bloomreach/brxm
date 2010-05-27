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
package org.hippoecm.hst.configuration.hosting;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostsManagerImpl implements VirtualHostsManager {

    private static final Logger log = LoggerFactory.getLogger(VirtualHostsManagerImpl.class);
    
    private Repository repository;
    private Credentials credentials;
    private VirtualHosts virtualHosts;
    private HstURLFactory urlFactory;
    private HstSiteMapMatcher siteMapMatcher;
    private HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public HstURLFactory getUrlFactory() {
        return this.urlFactory;
    }
    
    public void setUrlFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
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
                buildVirtualHosts();
                vHosts = this.virtualHosts;
            }
        }
        
        return vHosts;
    }

    protected synchronized void buildVirtualHosts() throws RepositoryNotAvailableException{
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
            
            // now, only a single node of type hst:virtualhosts is allowed in the repository: search that node and build the configuration.
            
            String xpath = "//element(*, "+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS+")";
            QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator it = result.getNodes();
            if(it.getSize() != 1L) {
                log.error("There must be exactly one node of type '{}' in the repository. We found '{}' nodes of type hst:virtualhosts. Cannot build configuration.",HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS, String.valueOf(it.getSize()));
                return;
            }
            
            // there is 1 result!
            Node virtualHostsNode = it.nextNode();
            this.virtualHosts = new VirtualHostsService(virtualHostsNode, this);
           
        } catch (LoginException e) {
            if (log.isDebugEnabled()) {
                log.info("The repository is not (yet) available: {}", e.getMessage()); 
            } 
            throw new RepositoryNotAvailableException("The repository is not (yet) available.", e );
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.error("Failed to retrieve virtual hosts configuration: {}, {}", e.getMessage(), e);
            } else if (log.isWarnEnabled()) {
                log.error("Failed to retrieve site configuration: {}", e.getMessage());
            }
        } finally {
            if (session != null) {
                try { session.logout(); } catch (Exception ce) {}
            }
        }
    }
    

    public synchronized void invalidate(String path) {
        this.virtualHosts = null;
    }


}
