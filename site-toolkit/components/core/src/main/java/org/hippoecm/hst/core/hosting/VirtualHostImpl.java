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

import org.hippoecm.hst.core.hosting.VirtualHost;

public class VirtualHostImpl implements VirtualHost {
    
    private String hostName;
    private String siteName;
    private VirtualHosts virtualHosts;
    
    public VirtualHostImpl(VirtualHosts virtualHosts, String hostName, String siteName) {
        this.hostName = hostName;
        this.siteName = siteName;
        this.virtualHosts = virtualHosts;
    }

    public String getHostName() {
        return this.hostName;
    }
    
    public String getSiteName() {
        return this.siteName;
    }

    public VirtualHosts getVirtualHosts() {
        return this.virtualHosts;
    }

}
