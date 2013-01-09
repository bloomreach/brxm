/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.service.ServiceException;
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
    
    public PortMountService(HstNode portMount, VirtualHost virtualHost, HstManagerImpl hstManager) throws ServiceException {
        String nodeName = portMount.getValueProvider().getName();
        try {
            portNumber = Integer.parseInt(nodeName);
            if(portNumber < 1) {
                throw new ServiceException("Not allowed PortMount name '"+nodeName+"' : PortMount must be a positive integer larger than 0");
            }
        } catch(NumberFormatException e) {
            throw new ServiceException("Not allowed PortMount name '"+nodeName+"' : PortMount must be a positive integer larger than 0");
        }
        
        HstNode mount = portMount.getNode(HstNodeTypes.MOUNT_HST_ROOTNAME);
        if(mount != null && HstNodeTypes.NODETYPE_HST_MOUNT.equals(mount.getNodeTypeName())) {
            try {
                rootMount = new MountService(mount, null, virtualHost, hstManager, portNumber);
            } catch (ServiceException e) {
                log.error("The host '"+virtualHost.getHostName()+"' for port '"+portNumber+"' contains an incorrect configured Mount. The host with port cannot be used for hst request processing", e);
            } 
        }
        
    }
    
    public PortMountService(Mount rootMount, VirtualHost virtualHost) throws ServiceException {
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
