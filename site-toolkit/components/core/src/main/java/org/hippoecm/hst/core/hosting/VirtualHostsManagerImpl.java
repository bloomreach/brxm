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
package org.hippoecm.hst.core.hosting;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostsManagerImpl implements VirtualHostsManager{

    private static final Logger log = LoggerFactory.getLogger(VirtualHostsManagerImpl.class);
    
    protected Repository repository;
    protected Credentials credentials;
    protected VirtualHosts virtualHosts;
    private String defaultSiteName;
    private String virtualHostsPath;
    
    public VirtualHostsManagerImpl(String defaultSiteName, String virtualHostsPath) {
        this.defaultSiteName = defaultSiteName;
        this.virtualHostsPath = virtualHostsPath;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
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
            
            if(virtualHostsPath != null && virtualHostsPath.startsWith("/") && session.itemExists(virtualHostsPath)) {
                Item item = session.getItem(virtualHostsPath);
                if(!item.isNode()) {
                    log.error("Failed to retrieve virtual hosts configuration because '{}' is not pointing to a node but a property", virtualHostsPath);
                    return;
                }
                Node virtualHostsNode = (Node)item;
                if(!virtualHostsNode.isNodeType(Configuration.NODETYPE_HST_VIRTUALHOSTS)) {
                    log.error("Failed to retrieve virtual hosts configuration because '{}' is not pointing to a node of type '{}'", virtualHostsPath, Configuration.NODETYPE_HST_VIRTUALHOSTS);
                    return;
                }
                this.virtualHosts = new VirtualHostsService(virtualHostsNode);
            } else {
                log.debug("No correct virtualhosts configured at {}. Use the default site for any request regardless the hostname.", virtualHostsPath);
                this.virtualHosts = new VirtualHostsService(defaultSiteName);
            }
            
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
