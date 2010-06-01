package org.hippoecm.hst.configuration.hosting;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMountService extends AbstractJCRService implements PortMount , Service {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PortMountService.class);
    
    /**
     * The portNumber of this PortMount
     */
    private int portNumber;
    
    /**
     * The root sitemount for this PortMount
     */
    private SiteMount rootSiteMount;
    
    public PortMountService(Node portMount, VirtualHost virtualHost) throws ServiceException {
        super(portMount);
        String nodeName = this.getValueProvider().getName();
        try {
            portNumber = Integer.parseInt(nodeName);
            if(portNumber < 1) {
                throw new ServiceException("Not allowed PortMount name '"+nodeName+"' : PortMount must be a positive integer larger than 0");
            }
        } catch(NumberFormatException e) {
            throw new ServiceException("Not allowed PortMount name '"+nodeName+"' : PortMount must be a positive integer larger than 0");
        }
        
        try {
            if(portMount.hasNode(HstNodeTypes.SITEMOUNT_HST_ROOTNAME) && portMount.getNode(HstNodeTypes.SITEMOUNT_HST_ROOTNAME).isNodeType(HstNodeTypes.NODETYPE_HST_SITEMOUNT)) {
                // we have a configured root sitemount node without portmount. Let's populate this sitemount. This site mount will be added to 
                // a portmount service with portnumber 0, which means any port
                Node siteMount = portMount.getNode(HstNodeTypes.SITEMOUNT_HST_ROOTNAME);
                rootSiteMount = new SiteMountService(siteMount, null, virtualHost);  
            }
        } catch (ServiceException e) {
            log.warn("The host '{}' for port '"+portNumber+"' contains an incorrect configured SiteMount. The host with port cannot be used for hst request processing: {}", virtualHost.getHostName(), e.getMessage());
        } catch (RepositoryException e) {
            throw new ServiceException("Error during creating sitemounts: ", e);
        }
    }
    
    public PortMountService(SiteMount rootSiteMount, VirtualHost virtualHost) throws ServiceException {
        super(null);
        this.rootSiteMount = rootSiteMount;
        // the default portnumber is 0 by definition, which means port agnostic
        this.portNumber = 0;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public SiteMount getRootSiteMount() {
        return rootSiteMount;
    }


    public Service[] getChildServices() {
        if(rootSiteMount == null) {
            return new Service[0];
        }
        Service[] services = {(Service)rootSiteMount};
        return services;
    }
    

}
