package org.hippoecm.hst.configuration.components;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstComponentsConfigurationService extends AbstractJCRService implements HstComponentsConfiguration, Service{

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentsConfiguration.class);
    
    private Map<String, HstComponentConfiguration> componentConfigurations;
    private String componentsNodePath;
    
    public HstComponentsConfigurationService(Node componentsNode) throws RepositoryException {
        super(componentsNode);
        this.componentsNodePath = componentsNode.getPath();
        this.componentConfigurations = new HashMap<String, HstComponentConfiguration>();
        init(componentsNode);
    }
     
    public Service[] getChildServices() {
        return componentConfigurations.values().toArray(new Service[componentConfigurations.size()]);
    }

    
    public HstComponentConfiguration getComponentConfiguration(String id) {
        return this.componentConfigurations.get(id);
    }
    

    public Map<String, HstComponentConfiguration> getComponentConfigurations() {
        return this.componentConfigurations;
    }

    
    private void init(Node node) throws RepositoryException {
        
        for(NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext();) {
            Node child = nodeIt.nextNode();
            if(child == null) {
                log.warn("skipping null node");
                continue;
            }
            if(child.isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
                if(child.hasProperty(Configuration.COMPONENT_PROPERTY_REFERECENCENAME)) {
                    try {
                        HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(child, componentsNodePath);
                        componentConfigurations.put(componentConfiguration.getId(), componentConfiguration);
                        log.debug("Added component service with key '{}'",componentConfiguration.getId());
                    } catch (ServiceException e) {
                        log.warn("Skipping component '{}'", child.getPath(), e);
                    }
                    
                    
                } else {
                    log.warn("Skipping '{}' hst:component + child components because it does not contain the mandatory property '{}'",Configuration.COMPONENT_PROPERTY_REFERECENCENAME, child.getPath());
                }
            } else if(child.isNodeType(Configuration.NODETYPE_HST_COMPONENTGROUP)) {
                init(child);
            }
            else {
                log.warn("Skipping node '{}' because is not of type '{}'", child.getPath(), (Configuration.NODETYPE_HST_COMPONENT + " | " + Configuration.NODETYPE_HST_COMPONENTGROUP));
            }
        }
    }

}
