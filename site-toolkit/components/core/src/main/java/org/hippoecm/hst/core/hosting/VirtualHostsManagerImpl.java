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
import javax.jcr.Repository;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostsManagerImpl implements VirtualHostsManager{

    static Logger log = LoggerFactory.getLogger(VirtualHostsManagerImpl.class);
    
    protected Repository repository;
    protected Credentials credentials;
    protected VirtualHosts virtualHosts;
    private String defaultSiteName;
    
    public VirtualHostsManagerImpl(String defaultSiteName) {
        this.defaultSiteName = defaultSiteName;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    
    public VirtualHosts getVirtualHosts() {
        if (this.virtualHosts == null) {
            buildVirtualHosts();
        }
        
        return this.virtualHosts;
    }

    protected synchronized void buildVirtualHosts() {
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
            this.virtualHosts = new VirtualHostsImpl(defaultSiteName, session);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve virtual hosts configuration: {}, {}", e.getMessage(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to retrieve site configuration: {}", e.getMessage());
            }
        } finally {
            if (session != null) {
                try { session.logout(); } catch (Exception ce) {}
            }
        }
    }
    

    public void invalidate(String path) {
        
    }

}
