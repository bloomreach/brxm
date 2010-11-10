package org.hippoecm.hst.configuration.hosting;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMountService implements PortMount {

    private static final long serialVersionUID = 1L;
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
                log.warn("The host '{}' for port '"+portNumber+"' contains an incorrect configured Mount. The host with port cannot be used for hst request processing: {}", virtualHost.getHostName(), e.getMessage());
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

}
