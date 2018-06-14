/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.hosting;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.MutablePortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMountService implements MutablePortMount {

    private static final Logger log = LoggerFactory.getLogger(PortMountService.class);
    
    /**
     * The portNumber of this PortMount
     */
    private int portNumber;
    
    /**
     * The root {@link Mount} for this PortMount
     */
    private Mount rootMount;
    
    public PortMountService(final HstNode portMount, final VirtualHost virtualHost, final HstNodeLoadingCache hstNodeLoadingCache,
                            final HstConfigurationLoadingCache hstConfigurationLoadingCache) throws ModelLoadingException {
        String nodeName = portMount.getValueProvider().getName();
        try {
            portNumber = Integer.parseInt(nodeName);
            if(portNumber < 1) {
                throw new ModelLoadingException("Not allowed PortMount name '"+nodeName+"' : PortMount must be a positive integer larger than 0");
            }
        } catch(NumberFormatException e) {
            throw new ModelLoadingException("Not allowed PortMount name '"+nodeName+"' : PortMount must be a positive integer larger than 0");
        }
        
        HstNode mount = portMount.getNode(HstNodeTypes.MOUNT_HST_ROOTNAME);
        if(mount != null && HstNodeTypes.NODETYPE_HST_MOUNT.equals(mount.getNodeTypeName())) {
            rootMount = new MountService(mount, null, virtualHost, hstNodeLoadingCache, hstConfigurationLoadingCache, portNumber);
        }
    }
    
    public PortMountService(Mount rootMount) {
        this.rootMount = rootMount;
        // the default portnumber is 0 by definition, which means port agnostic
        this.portNumber = 0;
    }


    public int getPortNumber() {
        return portNumber;
    }

    public Mount getRootMount() {
        return rootMount;
    }

    @Override
    public void setRootMount(MutableMount mount) {
        this.rootMount = mount;
    }

    @Override
    public String toString() {
        return "PortMountService [portNumber=" + portNumber + "]";
    }
    
}
