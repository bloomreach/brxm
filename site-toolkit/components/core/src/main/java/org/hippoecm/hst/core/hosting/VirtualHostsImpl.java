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

import java.util.List;

import org.hippoecm.hst.core.hosting.VirtualHost;
import org.hippoecm.hst.core.hosting.VirtualHosts;

public class VirtualHostsImpl implements VirtualHosts {
    
    protected List<VirtualHost> virtualHosts;
    protected String defaultSiteName;

    public VirtualHostsImpl(List<VirtualHost> virtualHosts) {
        this.virtualHosts = virtualHosts;
    }
    
    public void setDefaultSiteName(String defaultSiteName) {
        this.defaultSiteName = defaultSiteName;
    }
    
    public List<VirtualHost> getVirtualHosts() {
        return this.virtualHosts;
    }

    public VirtualHost findVirtualHost(String hostName) {
        VirtualHost virtualHost = null;
        
        for (VirtualHost host : this.virtualHosts) {
            if (host.getHostName().equals(hostName)) {
                virtualHost = host;
                break;
            }
        }
        
        if (virtualHost == null && this.defaultSiteName != null) {
            virtualHost = new VirtualHostImpl(hostName, this.defaultSiteName);
        }
        
        return virtualHost;
    }

}
